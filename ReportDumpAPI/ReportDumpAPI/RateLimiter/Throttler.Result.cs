namespace ReportDumpAPI.RateLimiter
{
    public partial class Throttle
    {
        public enum Result
        {
            Proceed,
            RejectSoft,
            RejectHard
        }
    }
}