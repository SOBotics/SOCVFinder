using System;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using ReportDumpAPI.RateLimiter;
using ServiceStack;

namespace ReportDumpAPI.ApiServer
{
    public partial class Server : IDisposable
    {
        private const int lpsPort = 86;
        private readonly string lpsConnectionData = $"http://{Config.FQD}/api {lpsPort}";
        private readonly bool onMono = Type.GetType("Mono.Runtime") != null;
        private HttpListener socket;
        private bool dispose;

        public static RegexOptions RegexOpts => RegexOptions.Compiled | RegexOptions.CultureInvariant;

        public Throttle Throttler { get; private set; }

        public Action<Exception> OnException { get; set; }

        public event Action<string> NewReport;



        public Server()
        {
            Throttler = new Throttle
            {
                //TOD: move this to a config file.
                MaxConcurrentIPs = 50,
                MaxReqsPerMinPerIP = 120
            };

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



        private void StartSocket()
        {
            socket = new HttpListener();
#if LPS
            socket.Prefixes.Add($"http://localhost:{lpsPort}/");

            new WebClient().ConnectToLpsServer(lpsConnectionData);
#else
            socket.Prefixes.Add($"http://{Config.FQD}/api/");
#endif
            socket.Start();

            Task.Run(() => ListenLoop());
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
                                client.Response.StatusCode = 429;
                                var msg = Encoding.UTF8.GetBytes("You have exhausted your request allowance. Wait a minute before trying again.");
                                client.Response.OutputStream.Write(msg, 0, msg.Length);
                                client.Response.Close();
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
                byte[] data;

                if (client.Request.RawUrl != "/api/dump-report" || client.Request.HttpMethod != "POST")
                {
                    var msg = "The requested resource could not be found.";
                    client.SendResponseAndClose(msg, 404);
                    return;
                }
                else
                {
                    var sc = 200;
                    var reqBody = client.Request.InputStream.ReadFully();
                    var isZipped = client.Request.Headers["Content-Encoding"]?.Contains("gzip") ?? false;
                    var strData = RequestHandler.HandleReportRequest(reqBody, isZipped, out sc);

                    client.Response.StatusCode = sc;
                    data = Encoding.UTF8.GetBytes(strData);

                    if (sc == 200)
                    {
                        NewReport?.Invoke(strData);
                    }
                }

                WriteData(ref client, data);
            }
            // Ignore not being able to send the response if the client left.
            catch (HttpListenerException ex)
            // Mono throws error code ???, .NET throws error code 64.
            when (/*(onMono && ex.NativeErrorCode == 0) || */(!onMono && (ex.NativeErrorCode == 64 || ex.NativeErrorCode == 1229)))
            { }
            catch (Exception ex)
            {
                OnException?.Invoke(ex);
                try
                {
                    var msg = "An error occurred while processing your request.";
                    client.SendResponseAndClose(msg, 500);
                }
                catch
                {
                    client.Response.Abort();
                }
            }
        }

        private void WriteData(ref HttpListenerContext c, byte[] data)
        {
            if (c.Request.Headers["Accept-Encoding"]?.Contains("gzip") ?? false)
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
    }
}