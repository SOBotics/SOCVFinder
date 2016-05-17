using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ServiceStack;
using ServiceStack.Text;

namespace ReportDumpAPI
{
    public static class Config
    {
        private const string configFile = "config.json";

        public static string ReportDataDir { get; set; }

        public static string FQD { get; set; }



        static Config()
        {
            if (!File.Exists(configFile))
            {
                throw new FileNotFoundException("The configuration file could not be found.", configFile);
            }

            var json = File.ReadAllText(configFile);
            var data = JsonSerializer.DeserializeFromString<Dictionary<string, object>>(json);

            ReportDataDir = data["ReportDataDir"].ToString();
            FQD = data["FQD"].ToString();

            if (!Directory.Exists(ReportDataDir))
            {
                Directory.CreateDirectory(ReportDataDir);
            }
        }
    }
}
