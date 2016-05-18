using System;
using System.Collections.Concurrent;
using System.IO;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using ReportDumpAPI.RateLimiter;

namespace ReportDumpAPI.WebServer
{
    class Server
    {
        private const int lpsPort = 87;
        private readonly string lpsConnectionData = $"http://{Config.FQD} {lpsPort}";
        private readonly bool onMono = Type.GetType("Mono.Runtime") != null;
        private HttpListener socket;
        private ConcurrentDictionary<string, byte[]> content;
        private bool dispose;

        public Throttle Throttler { get; private set; }

        public Action<Exception> OnException { get; set; }



        public Server()
        {
            var dirToServe = Config.ReportDataDir;

            if (!Directory.Exists(dirToServe))
            {
                throw new DirectoryNotFoundException($"Could not find the directory: {dirToServe}");
            }

            Throttler = new Throttle
            {
                MaxConcurrentIPs = 50,
                MaxReqsPerMinPerIP = 120
            };

            InitialiseContent(new FileInfo(dirToServe).FullName);
            StartSocket();
        }

        ~Server()
        {
            Dispose();
        }



        public void Dispose()
        {
            if (dispose) return;
            dispose = true;
#if LPS
            new WebClient().DisconnectFromLpsServer(lpsConnectionData);
#endif
            socket.Stop();

            GC.SuppressFinalize(this);
        }

        public void AddNewReport(string reportUrl)
        {
            var pq = new Uri(reportUrl).PathAndQuery;
            var fileName = (pq + ".html").Replace('/', Path.DirectorySeparatorChar).Remove(0, 1);
            var file = Path.Combine(Config.ReportDataDir, fileName);
            var key = "/" + pq + ".html";

            content[key] = File.ReadAllBytes(file);
        }



        private void StartSocket()
        {
            socket = new HttpListener();
#if LPS
            socket.Prefixes.Add($"http://localhost:{lpsPort}/");

            new WebClient().ConnectToLpsServer(lpsConnectionData);
#else
            socket.Prefixes.Add($"http://{Config.FQD}/")
#endif
            socket.Start();

            Task.Run(() => ListenLoop());
        }

        private void InitialiseContent(string dir)
        {
            content = new ConcurrentDictionary<string, byte[]>();

            var files = Directory.EnumerateFiles(dir, "*", SearchOption.AllDirectories);

            foreach (var file in files)
            {
                var key = file.Remove(0, dir.Length).Replace(Path.DirectorySeparatorChar, '/');

                content[key] = File.ReadAllBytes(file);
            }
        }

        private void ListenLoop()
        {
            while (!dispose)
            {
                try
                {
                    var client = socket.GetContext();

                    Task.Run(() =>
                    {
                        var res = Throttler.Process(client.Request.RemoteEndPoint.Address);

                        if (res != Throttle.Result.Proceed)
                        {
                            if (res == Throttle.Result.RejectSoft)
                            {
                                var msg = "You have exhausted your request allowance. Wait a minute before trying again.";
                                client.SendResponseAndClose(msg, 429);
                            }
                            else
                            {
                                client.Response.Abort();
                            }
                        }
                        else
                        {
                            HandleClient(client);
                        }
                    });
                }
                // Ignore exceptions thrown whilst disposing.
                catch (HttpListenerException ex)
                // Mono throws 500, .NET throws 995.
                when ((onMono && ex.NativeErrorCode == 500) || (!onMono && ex.NativeErrorCode == 995))
                { }
                // Log everything else.
                catch (Exception ex)
                {
                    OnException?.Invoke(ex);
                }
            }
        }

        private void HandleClient(HttpListenerContext client)
        {
            try
            {
                var requestedResource = WebUtility.UrlDecode(client.Request.RawUrl);
                var ext = Path.GetExtension(requestedResource).ToUpperInvariant();

                if (string.IsNullOrEmpty(ext))
                {
                    requestedResource += ".html";
                }

                var data = new byte[0];

                using (var resStrm = client.Response.OutputStream)
                {
                    if (!content.ContainsKey(requestedResource))
                    {
                        var msg = "The requested resource could not be found.";
                        client.SendResponseAndClose(msg, 404);
                        return;
                    }
                    else
                    {
                        data = content[requestedResource];
                    }

                    SetHeaders(ref client, requestedResource);
                    WriteData(ref client, data);
                }
            }
            // Ignore not being able to send the response if the client left.
            catch (HttpListenerException ex)
            // Mono throws error code ???, .NET throws error code 64.
            when (/*(onMono && ex.NativeErrorCode == 0) || */(!onMono && (ex.NativeErrorCode == 64 || ex.NativeErrorCode == 1229)))
            { }
            catch (Exception ex)
            {
                OnException?.Invoke(ex);
            }
        }

        private void SetHeaders(ref HttpListenerContext c, string resource)
        {
            if (c.Response.StatusCode != 200) return;

            c.Response.Headers["Cache-Control"] = GetCacheControlMaxAge(resource);
            c.Response.ContentType = GetMimeType(resource);
        }

        private void WriteData(ref HttpListenerContext c, byte[] data)
        {
            if (c.Request.Headers["Accept-Encoding"]?.Contains("gzip") ?? false && !IsCompressedResource(c))
            {
                var zipped = data.Compress();

                c.Response.Headers["Content-Encoding"] = "gzip";
                c.Response.ContentLength64 = zipped.LongLength;
                c.Response.OutputStream.Write(zipped, 0, zipped.Length);
            }
            else
            {
                c.Response.ContentLength64 = data.LongLength;
                c.Response.OutputStream.Write(data, 0, data.Length);
            }

            c.Response.OutputStream.Close();
        }

        private string GetMimeType(string res)
        {
            return "text/json"; //TODO: Temporary until I implement PageGenerator.

            switch (Path.GetExtension(res).ToUpperInvariant())
            {
                case "":
                {
                    return "text/html; charset=utf-8";
                }
                case ".HTML":
                {
                    return "text/html; charset=utf-8";
                }
                case ".JS":
                {
                    return "text/javascript;";
                }
                case ".CSS":
                {
                    return "text/css;";
                }
                case ".JPG":
                {
                    return "image/jpg";
                }
                case ".JPEG":
                {
                    return "image/jpg";
                }
                case ".PNG":
                {
                    return "image/png";
                }
                case ".GIF":
                {
                    return "image/gif";
                }
                case ".ICO":
                {
                    return "image/x-icon";
                }
                default:
                {
                    return null;
                }
            }
        }

        private string GetCacheControlMaxAge(string res)
        {
            var ext = Path.GetExtension(res).ToUpperInvariant();

            if (new[] { ".JS", ".CSS" }.Contains(ext))
            {
                return "max-age=1800"; // 30 mins.
            }

            if (new[] { ".ICO", ".PNG", ".JPG", ".JPEG", ".SVG" }.Contains(ext))
            {
                return "max-age=604800"; // 1 week.
            }

            if (new[] { ".EOT", ".TTF", ".WOFF", ".WOFF2" }.Contains(ext))
            {
                return "max-age=2592000"; // 1 month.
            }

            return "max-age=0";
        }

        private bool IsCompressedResource(HttpListenerContext c)
        {
            var type = c.Response.ContentType;

            switch (type)
            {
                case "image/jpg":
                {
                    return true;
                }
                case "image/png":
                {
                    return true;
                }
                case "image/gif":
                {
                    return true;
                }
            }

            return false;
        }
    }
}
