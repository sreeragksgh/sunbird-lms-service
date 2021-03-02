package org.sunbird.location.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.location.dao.LocationDao;
import org.sunbird.location.dao.impl.LocationDaoFactory;
import org.sunbird.location.util.LocationRequestValidator;
import org.sunbird.models.location.Location;
import org.sunbird.models.location.LocationIdType;
import org.sunbird.models.location.apirequest.UpsertLocationRequest;

/**
 * This class will handle all location related request.
 *
 * @author Amit Kumar
 */
@ActorConfig(
  tasks = {
    "createLocation",
    "updateLocation",
    "searchLocation",
    "deleteLocation",
    "getRelatedLocationIds",
    "getRelatedLocationIdsAndType"
  },
  asyncTasks = {}
)
public class LocationActor extends BaseLocationActor {

  private LocationDao locationDao = LocationDaoFactory.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.LOCATION);
    String operation = request.getOperation();
    switch (operation) {
      case "createLocation":
        createLocation(request);
        break;
      case "updateLocation":
        updateLocation(request);
        break;
      case "searchLocation":
        searchLocation(request);
        break;
      case "deleteLocation":
        deleteLocation(request);
        break;
      case "getRelatedLocationIds":
        getRelatedLocationIds(request);
        break;
      case "getRelatedLocationIdsAndType":
        getRelatedLocationIdsAndTypes(request);
        break;
      default:
        onReceiveUnsupportedOperation("LocationActor");
    }
  }

  private void getRelatedLocationIds(Request request) {
    Response response = new Response();
    List<String> relatedLocationIds =
        getValidatedRelatedLocationIds(
            (List<String>) request.get(JsonKey.LOCATION_CODES), request.getRequestContext());
    response.getResult().put(JsonKey.RESPONSE, relatedLocationIds);
    sender().tell(response, self());
  }

  private void getRelatedLocationIdsAndTypes(Request request) {
    Response response = new Response();
    List<Map<String, String>> relatedLocationIdsAndType =
            getValidatedRelatedLocationIdsAndType(
                    (List<String>) request.get(JsonKey.LOCATION_CODES), request.getRequestContext());
    response.getResult().put(JsonKey.RESPONSE, relatedLocationIdsAndType);
    sender().tell(response, self());
  }


  private void createLocation(Request request) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      UpsertLocationRequest locationRequest =
          ProjectUtil.convertToRequestPojo(request, UpsertLocationRequest.class);
      validateUpsertLocnReq(locationRequest, JsonKey.CREATE);
      // put unique identifier in request for Id
      String id = ProjectUtil.generateUniqueId();
      locationRequest.setId(id);
      Location location = mapper.convertValue(locationRequest, Location.class);
      Response response = locationDao.create(location, request.getRequestContext());
      sender().tell(response, self());
      logger.info(request.getRequestContext(), "Insert location data to ES");
      saveDataToES(
          mapper.convertValue(location, Map.class), JsonKey.INSERT, request.getRequestContext());
      generateTelemetryForLocation(
          id, mapper.convertValue(location, Map.class), JsonKey.CREATE, request.getContext());
    } catch (Exception ex) {
      logger.error(request.getRequestContext(), ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void updateLocation(Request request) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      UpsertLocationRequest locationRequest =
          ProjectUtil.convertToRequestPojo(request, UpsertLocationRequest.class);
      validateUpsertLocnReq(locationRequest, JsonKey.UPDATE);
      Location location = mapper.convertValue(locationRequest, Location.class);
      Response response = locationDao.update(location, request.getRequestContext());
      sender().tell(response, self());
      logger.info(request.getRequestContext(), "Update location data to ES");
      saveDataToES(
          mapper.convertValue(location, Map.class), JsonKey.UPDATE, request.getRequestContext());
      generateTelemetryForLocation(
          location.getId(),
          mapper.convertValue(location, Map.class),
          JsonKey.UPDATE,
          request.getContext());
    } catch (Exception ex) {
      logger.error(request.getRequestContext(), ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void searchLocation(Request request) {
    try {
      Response response = searchLocation(request.getRequest(), request.getRequestContext());
      sender().tell(response, self());
      SearchDTO searchDto = Util.createSearchDto(request.getRequest());
      String[] types = {ProjectUtil.EsType.location.getTypeName()};
      generateSearchTelemetryEvent(searchDto, types, response.getResult(), request.getContext());
    } catch (Exception ex) {
      logger.error(request.getRequestContext(), ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private Response searchLocation(Map<String, Object> searchMap, RequestContext context) {
    return locationDao.search(searchMap, context);
  }

  private void deleteLocation(Request request) {
    try {
      String locationId = (String) request.getRequest().get(JsonKey.LOCATION_ID);
      LocationRequestValidator.isLocationHasChild(locationId);
      Response response = locationDao.delete(locationId, request.getRequestContext());
      sender().tell(response, self());
      logger.info(request.getRequestContext(), "Delete location data from ES");
      deleteDataFromES(locationId, request.getRequestContext());
      generateTelemetryForLocation(
          locationId, new HashMap<>(), JsonKey.DELETE, request.getContext());
    } catch (Exception ex) {
      logger.error(request.getRequestContext(), ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void saveDataToES(Map<String, Object> locData, String opType, RequestContext context) {
    Request request = new Request();
    request.setRequestContext(context);
    request.setOperation(LocationActorOperation.UPSERT_LOCATION_TO_ES.getValue());
    request.getRequest().put(JsonKey.LOCATION, locData);
    request.getRequest().put(JsonKey.OPERATION_TYPE, opType);
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      logger.error(context, "LocationActor:saveDataToES: Exception occurred", ex);
    }
  }

  private void deleteDataFromES(String locId, RequestContext context) {
    Request request = new Request();
    request.setRequestContext(context);
    request.setOperation(LocationActorOperation.DELETE_LOCATION_FROM_ES.getValue());
    request.getRequest().put(JsonKey.LOCATION_ID, locId);
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      logger.error(context, "LocationActor:saveDataToES: Exception occurred ", ex);
    }
  }

  private void validateUpsertLocnReq(UpsertLocationRequest locationRequest, String operation) {
    if (StringUtils.isNotEmpty(locationRequest.getType())) {
      LocationRequestValidator.isValidLocationType(locationRequest.getType());
    }
    LocationRequestValidator.isValidParentIdAndCode(locationRequest, operation);
  }

  public List<String> getValidatedRelatedLocationIds(
      List<String> codeList, RequestContext context) {
    Set<String> locationIds = null;
    List<String> codes = new ArrayList<>(codeList);
    List<Location> locationList = getSearchResult(JsonKey.CODE, codeList, context);
    List<String> locationIdList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(locationList)) {
      if (locationList.size() != codes.size()) {
        List<String> resCodeList =
            locationList.stream().map(Location::getCode).collect(Collectors.toList());
        List<String> invalidCodeList =
            codes.stream().filter(s -> !resCodeList.contains(s)).collect(Collectors.toList());
        throwInvalidParameterValueException(invalidCodeList);
      } else {
        locationIds = getValidatedRelatedLocationSet(locationList, context);
      }
    } else {
      throwInvalidParameterValueException(codeList);
    }
    locationIdList.addAll(locationIds);
    return locationIdList;
  }

  public List<Map<String, String>> getValidatedRelatedLocationIdsAndType(
          List<String> codeList, RequestContext context) {
    Set<Map<String,String>> locationIdType = null;
    List<String> codes = new ArrayList<>(codeList);
    List<LocationIdType> locationList = getSearchResultLocationIdType(JsonKey.CODE, codeList, context);
    List<Map<String, String>> locationIdTypeList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(locationList)) {
      if (locationList.size() != codes.size()) {
        List<String> resCodeList =
                locationList.stream().map(LocationIdType::getCode).collect(Collectors.toList());
        List<String> invalidCodeList =
                codes.stream().filter(s -> !resCodeList.contains(s)).collect(Collectors.toList());
        throwInvalidParameterValueException(invalidCodeList);
      } else {
        locationIdType = (Set<Map<String, String>>) getValidatedRelatedLocationIdTypeSet(locationList, context);
      }
    } else {
      throwInvalidParameterValueException(codeList);
    }
    locationIdTypeList.addAll((Collection<? extends Map<String, String>>) locationIdType);
    return locationIdTypeList;
  }


  private List<Location> getSearchResult(String param, Object value, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    filters.put(param, value);
    searchRequestMap.put(JsonKey.FILTERS, filters);
    Response response = searchLocation(searchRequestMap, context);
    if (response != null) {
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      ObjectMapper mapper = new ObjectMapper();
      return responseList
          .stream()
          .map(s -> mapper.convertValue(s, Location.class))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  private List<LocationIdType> getSearchResultLocationIdType(String param, Object value, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    filters.put(param, value);
    searchRequestMap.put(JsonKey.FILTERS, filters);
    Response response = searchLocation(searchRequestMap, context);
    if (response != null) {
      List<Map<String, LocationIdType>> responseList =
              (List<Map<String, LocationIdType>>) response.getResult().get(JsonKey.RESPONSE);
      ObjectMapper mapper = new ObjectMapper();
      return responseList
              .stream()
              .map(s -> mapper.convertValue(s, LocationIdType.class))
              .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }


  private Location getLocation(String locationId, RequestContext context) {
    List<Location> locations = getSearchResult(JsonKey.ID, locationId, context);
    if (locations.isEmpty()) return null;

    return locations.get(0);
  }

  private LocationIdType getLocationIdType(String locationId, RequestContext context) {
    List<LocationIdType> locations = getSearchResultLocationIdType(JsonKey.ID, locationId, context);
    if (locations.isEmpty()) return null;

    return locations.get(0);
  }


  private void throwInvalidParameterValueException(List<String> codeList) {
    throw new ProjectCommonException(
        ResponseCode.invalidParameterValue.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.invalidParameterValue.getErrorMessage(), codeList, JsonKey.LOCATION_CODE),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  public Map<String,String> getValidatedRelatedLocationIdTypeSet(
          List<LocationIdType> locationList, RequestContext context) {
    Set<LocationIdType> locationIdTypeSet = new HashSet<>();
    for (LocationIdType requestedLocation : locationList) {
      Set<LocationIdType> parentLocnSet = getParentLocations(requestedLocation, context);
      if (CollectionUtils.sizeIsEmpty(locationIdTypeSet)) {
        locationIdTypeSet.addAll(parentLocnSet);
      } else {
        for (LocationIdType currentLocation : parentLocnSet) {
          String type = currentLocation.getType();
          locationIdTypeSet
                  .stream()
                  .forEach(
                          location -> {
                            if (type.equalsIgnoreCase(location.getType())
                                    && !(currentLocation.getId().equals(location.getId()))) {
                              throw new ProjectCommonException(
                                      ResponseCode.conflictingOrgLocations.getErrorCode(),
                                      ProjectUtil.formatMessage(
                                              ResponseCode.conflictingOrgLocations.getErrorMessage(),
                                              requestedLocation.getCode(),
                                              location.getCode(),
                                              type),
                                      ResponseCode.CLIENT_ERROR.getResponseCode());
                            }
                          });
          locationIdTypeSet.add(currentLocation);
        }
      }
    }
    locationIdTypeSet.stream().map(LocationIdType::getType).collect(Collectors.toSet());
    locationIdTypeSet.stream().map(LocationIdType::getId).collect(Collectors.toSet());
    return (Map<String, String>) locationIdTypeSet;
  }


  public Set<String> getValidatedRelatedLocationSet(
      List<Location> locationList, RequestContext context) {
    Set<Location> locationSet = new HashSet<>();
    for (Location requestedLocation : locationList) {
      Set<Location> parentLocnSet = getParentLocations(requestedLocation, context);
      if (CollectionUtils.sizeIsEmpty(locationSet)) {
        locationSet.addAll(parentLocnSet);
      } else {
        for (Location currentLocation : parentLocnSet) {
          String type = currentLocation.getType();
          locationSet
              .stream()
              .forEach(
                  location -> {
                    if (type.equalsIgnoreCase(location.getType())
                        && !(currentLocation.getId().equals(location.getId()))) {
                      throw new ProjectCommonException(
                          ResponseCode.conflictingOrgLocations.getErrorCode(),
                          ProjectUtil.formatMessage(
                              ResponseCode.conflictingOrgLocations.getErrorMessage(),
                              requestedLocation.getCode(),
                              location.getCode(),
                              type),
                          ResponseCode.CLIENT_ERROR.getResponseCode());
                    }
                  });
          locationSet.add(currentLocation);
        }
      }
    }
    return locationSet.stream().map(Location::getId).collect(Collectors.toSet());
  }

  private Set<Location> getParentLocations(Location locationObj, RequestContext context) {
    Set<Location> locationSet = new LinkedHashSet<>();
    Location location = locationObj;
    int count = getOrder(location.getType());
    locationSet.add(location);
    while (count > 0) {
      Location parent = null;
      if (getOrder(location.getType()) == 0 && StringUtils.isNotEmpty(location.getId())) {
        parent = getLocation(location.getId(), context);
      } else if (StringUtils.isNotEmpty(location.getParentId())) {
        parent = getLocation(location.getParentId(), context);
      }
      if (null != parent) {
        locationSet.add(parent);
        location = parent;
      }
      count--;
    }
    return locationSet;
  }

  public int getOrder(String type) {
    return DataCacheHandler.getLocationOrderMap().get(type);
  }

  private Set<LocationIdType> getParentLocations(LocationIdType locationObj, RequestContext context) {
    Set<LocationIdType> locationSet = new LinkedHashSet<>();
    LocationIdType location = locationObj;
    int count = getOrder(location.getType());
    locationSet.add(location);
    while (count > 0) {
      LocationIdType parent = null;
      if (getOrder(location.getType()) == 0 && StringUtils.isNotEmpty(location.getId())) {
        parent = getLocationIdType(location.getId(), context);
      } else if (StringUtils.isNotEmpty(location.getParentId())) {
        parent = getLocationIdType(location.getParentId(), context);
      }
      if (null != parent) {
        locationSet.add(parent);
        location = parent;
      }
      count--;
    }
    return locationSet;
  }

}
