using System;

namespace ReportDumpAPI.RateLimiter
{
    public partial class Throttle
    {
        private class Req
        {
            public Guid ID { get; private set; }
            public bool Processed { get; set; }
            public DateTime? Requested { get; set; }



            public Req()
            {
                ID = Guid.NewGuid();
            }
        }
    }
}