using System;
using System.Threading;

namespace ReportDumpAPI
{
    class Program
    {
        static void Main(string[] args)
        {
            var mre = new ManualResetEvent(false);
            Console.CancelKeyPress += (o, e) =>
            {
                e.Cancel = true;
                mre.Set();
            };

            Console.Write("Staring web server...");

            var webServer = new WebServer.Server()
            {
                OnException = e => Console.WriteLine(e)
            };

            Console.Write("done\nStaring API server...");

            var apiServer = new ApiServer.Server()
            {
                OnException = e => Console.WriteLine(e)
            };
            apiServer.NewReport += r => webServer.AddNewReport(r);

            Console.WriteLine("done\n");

            mre.WaitOne();
            Console.Write("Stopping...");

            mre.Dispose();
            apiServer.Dispose();
            webServer.Dispose();

            Console.WriteLine("done");
        }
    }
}
