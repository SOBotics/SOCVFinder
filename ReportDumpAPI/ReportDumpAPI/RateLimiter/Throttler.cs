using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Threading;
using System.Threading.Tasks;

namespace ReportDumpAPI.RateLimiter
{
    public partial class Throttle : IDisposable
    {
        private readonly ConcurrentDictionary<IPAddress, List<Req>> reqs = new ConcurrentDictionary<IPAddress, List<Req>>();
        private bool dispose;

        public int MaxConcurrentIPs { get; set; } = 50;
        public int MaxReqsPerMinPerIP { get; set; } = 90;



        public Throttle()
        {
            Task.Run(() => ReqsUpdater());
        }

        ~Throttle()
        {
            Dispose();
        }



        public void Dispose()
        {
            if (dispose) return;
            dispose = true;

            GC.SuppressFinalize(this);
        }

        public Result Process(IPAddress ip)
        {
            var req = new Req();

            if (reqs.ContainsKey(ip))
            {
                lock (reqs[ip])
                {
                    reqs[ip].Add(req);
                }
            }
            else
            {
                reqs[ip] = new List<Req> { req };
            }

            if (reqs.Count > MaxConcurrentIPs)
            {
                return reqs.Count > MaxConcurrentIPs * 1.5 ?
                    Result.RejectHard :
                    Result.RejectSoft;
            }

            if (reqs.ContainsKey(ip) && reqs[ip].Count > MaxReqsPerMinPerIP)
            {
                if (reqs[ip].Count > MaxReqsPerMinPerIP * 1.5)
                {
                    return Result.RejectHard;
                }
                else
                {
                    return Result.RejectSoft;
                }
            }

            req.Requested = DateTime.UtcNow;
            req.Processed = true;

            return Result.Proceed;
        }



        private void ReqsUpdater()
        {
            while (!dispose)
            {
                Thread.Sleep(100);

                lock (reqs)
                {
                    foreach (var k in reqs.Keys)
                    {
                        reqs[k] = reqs[k].Where(x => !x.Processed || (x.Requested != null && ((DateTime.UtcNow - x?.Requested)?.TotalMinutes ?? 0) < 1)).ToList();
                    }

                    foreach (var k in reqs.Keys)
                    {
                        if (reqs[k].Count != 0) continue;

                        List<Req> temp;
                        reqs.TryRemove(k, out temp);
                    }
                }
            }
        }
    }
}