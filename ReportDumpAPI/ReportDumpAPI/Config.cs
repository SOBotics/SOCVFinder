using System.Collections.Generic;
using System.IO;
using Jil;

namespace ReportDumpAPI
{
    public static class Config
    {
        private const string configFile = "config.json";

        public static string ContentDir { get; set; }

        public static string FQD { get; set; }

        public static int ReportLifeMaxDays { get; set; }



        static Config()
        {
            if (!File.Exists(configFile))
            {
                throw new FileNotFoundException("The configuration file could not be found.", configFile);
            }

            var json = File.ReadAllText(configFile);
            var data = JSON.Deserialize<Dictionary<string, string>>(json);

            ContentDir = data["ContentDir"];
            FQD = data["FQD"];
            ReportLifeMaxDays = int.Parse(data["ReportLifeMaxDays"]);

            if (!Directory.Exists(ContentDir))
            {
                Directory.CreateDirectory(ContentDir);
            }
        }
    }
}
