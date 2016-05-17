using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace ReportDumpAPI
{
    public static class Extensions
    {
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
