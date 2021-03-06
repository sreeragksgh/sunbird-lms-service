package org.sunbird.ratelimit.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.ratelimit.dao.RateLimitDao;
import org.sunbird.ratelimit.dao.RateLimitDaoImpl;
import org.sunbird.ratelimit.limiter.RateLimit;
import org.sunbird.ratelimit.limiter.RateLimiter;

public class RateLimitServiceImpl implements RateLimitService {
  private static LoggerUtil logger = new LoggerUtil(RateLimitServiceImpl.class);

  private RateLimitDao rateLimitDao = RateLimitDaoImpl.getInstance();

  public boolean isRateLimitOn() {
    return Boolean.TRUE
        .toString()
        .equalsIgnoreCase(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_RATE_LIMIT_ENABLED));
  }

  @Override
  public void throttleByKey(String key, RateLimiter[] rateLimiters, RequestContext context) {
    if (!isRateLimitOn()) {
      logger.info(context, "RateLimitServiceImpl:throttleByKey: Rate limiter is disabled");
      return;
    }
    Map<String, RateLimit> entryByRate = new HashMap<>();

    List<Map<String, Object>> ratesByKey = getRatesByKey(key, context);
    if (CollectionUtils.isNotEmpty(ratesByKey)) {
      ratesByKey
          .stream()
          .forEach(
              rate -> {
                if (!MapUtils.isEmpty(rate)) {
                  logger.info(
                      context,
                      "RateLimitServiceImpl:throttleByKey: key = " + key + " rate =" + rate);
                  RateLimit rateLimit = new RateLimit(key, rate);

                  if (rateLimit.getCount() >= rateLimit.getLimit()) {
                    logger.info(
                        context,
                        "RateLimitServiceImpl:throttleByKey: Rate limit threshold crossed for key = "
                            + key);
                    throw new ProjectCommonException(
                        ResponseCode.errorRateLimitExceeded.getErrorCode(),
                        ResponseCode.errorRateLimitExceeded.getErrorMessage(),
                        ResponseCode.TOO_MANY_REQUESTS.getResponseCode(),
                        rateLimit.getUnit().toLowerCase());
                  }
                  rateLimit.incrementCount();
                  entryByRate.put(rateLimit.getUnit(), rateLimit);
                }
              });
    }

    Arrays.stream(rateLimiters)
        .forEach(
            rateLimiter -> {
              if (!entryByRate.containsKey(rateLimiter.name())
                  && rateLimiter.getRateLimit() != null) {
                RateLimit rateLimit =
                    new RateLimit(
                        key, rateLimiter.name(), rateLimiter.getRateLimit(), rateLimiter.getTTL());
                logger.info(
                    context,
                    "RateLimitServiceImpl:throttleByKey: Initialise rate limit for key = "
                        + key
                        + " rate ="
                        + rateLimit.getLimit());
                entryByRate.put(rateLimiter.name(), rateLimit);
              }
            });

    rateLimitDao.insertRateLimits(new ArrayList<>(entryByRate.values()), context);
  }

  private List<Map<String, Object>> getRatesByKey(String key, RequestContext context) {
    return rateLimitDao.getRateLimits(key, context);
  }
}
