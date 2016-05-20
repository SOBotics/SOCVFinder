using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using ReportDumpAPI.WebServer;

namespace ReportDumpAPI.ReportPage
{
    public class PageDeleter : IDisposable
    {
        private readonly ManualResetEvent mre = new ManualResetEvent(false);
        private readonly TimeSpan pageTtl;
        private readonly Server webSrv;
        private bool dispose;

        public Action<Exception> OnException { get; set; }



        public PageDeleter(ref Server webServer, TimeSpan pageMaxLife)
        {
            webSrv = webServer;
            pageTtl = pageMaxLife;

            Task.Run(() => DeletePages());
        }



        public void Dispose()
        {
            if (dispose) return;
            dispose = true;

            mre.Set();
            mre.Dispose();

            GC.SuppressFinalize(this);
        }



        private void DeletePages()
        {
            while (!dispose)
            {
                try
                {
                    var files = Directory.EnumerateFiles(Config.ContentDir, "*.html", SearchOption.AllDirectories);

                    foreach (var file in files)
                    {
                        var fileName = Path.GetFileNameWithoutExtension(file);
                        var dirName = Path.GetFileName(Path.GetDirectoryName(file));

                        if (!dirName.All(char.IsDigit) || !fileName.All(char.IsDigit)) continue;

                        var fileTime = File.GetCreationTimeUtc(file);

                        if ((DateTime.UtcNow - fileTime) > pageTtl)
                        {
                            File.Delete(file);
                            var key = $"/{dirName}/{fileName}.html";
                            webSrv.RemoveReport(key);
                        }
                    }
                }
                catch (Exception ex)
                {
                    OnException?.Invoke(ex);
                }

                mre.WaitOne(TimeSpan.FromMinutes(5));
            }
        }
    }
}
