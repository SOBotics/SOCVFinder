using System.Collections.Generic;
using System.IO;
using Jil;

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
            var data = JSON.Deserialize<Dictionary<string, string>>(json);

            ReportDataDir = data["ReportDataDir"];
            FQD = data["FQD"];

            if (!Directory.Exists(ReportDataDir))
            {
                Directory.CreateDirectory(ReportDataDir);
            }
        }
    }
}
