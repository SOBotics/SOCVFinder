using System;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Text;

namespace ReportDumpAPI
{
    public static class Extensions
    {
        public static void ConnectToLpsServer(this WebClient wc, string data)
        {
            try
            {
                wc.UploadString("http://localhost:84/start", data);
            }
            catch (WebException ex1) when (ex1.Response != null && ex1.Response.ContentLength > 0)
            {
                using (var sr = new StreamReader(ex1.Response.GetResponseStream()))
                {
                    var ex1R = sr.ReadToEnd();

                    if (ex1R.StartsWith("A local client is already listening to"))
                    {
                        try
                        {
                            Console.Write("cleaning up last connection...");
                            new WebClient().DisconnectFromLpsServer(data);
                            Console.Write("done, re-attempting startup...");
                            wc.UploadString("http://localhost:84/start", data);
                        }
                        catch (WebException ex2) when (ex2.Response != null && ex2.Response.ContentLength > 0)
                        {
                            Console.WriteLine("failed.\n\n" + ex2);
                        }
                    }
                    else
                    {
                        throw new Exception("Unable to connect to LPS server. Response: " + ex1R, ex1);
                    }
                }
            }
        }
        public static void DisconnectFromLpsServer(this WebClient wc, string data)
        {
            try
            {
                wc.UploadString("http://localhost:84/stop", data);
            }
            catch (WebException ex) when (ex.Response != null && ex.Response.ContentLength > 0)
            {
                using (var sr = new StreamReader(ex.Response.GetResponseStream()))
                {
                    var exR = sr.ReadToEnd();

                    if (!exR.StartsWith("No client could be found listening to"))
                    {
                        throw new Exception("Unable to cleanly disconnect from the LPS server. Response: " + exR, ex);
                    }
                }
            }
        }

        public static void SendResponseAndClose(this HttpListenerContext c, string body, int statusCode)
        {
            c.Response.StatusCode = statusCode;

            if (statusCode != 204)
            {
                var b = Encoding.UTF8.GetBytes(body);
                c.Response.ContentLength64 = b.Length;
                c.Response.OutputStream.Write(b, 0, b.Length);
            }

            c.Response.Close();
        }

        public static byte[] Compress(this string str)
        {
            return Encoding.UTF8.GetBytes(str).Compress();
        }

        public static byte[] Compress(this byte[] bytes)
        {
            byte[] zipped;

            using (var compStrm = new MemoryStream())
            {
                using (var zipper = new GZipStream(compStrm, CompressionMode.Compress))
                using (var ms = new MemoryStream(bytes))
                {
                    ms.CopyTo(zipper);
                }

                zipped = compStrm.ToArray();
            }

            return zipped;
        }

        public static string Decompress(this byte[] zipped)
        {
            using (var inStrm = new MemoryStream(zipped))
            {
                return inStrm.Decompress();
            }
        }

        public static string Decompress(this Stream strm)
        {
            byte[] unzipped;
            using (var unzipper = new GZipStream(strm, CompressionMode.Decompress))
            using (var msOut = new MemoryStream())
            {
                unzipper.CopyTo(msOut);
                unzipped = msOut.ToArray();
            }

            return Encoding.UTF8.GetString(unzipped);
        }
    }
}
