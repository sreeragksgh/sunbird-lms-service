package controllers.location;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.TestUtil;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.RequestInterceptor;

/** @author arvind on 19/4/18. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(OnRequestHandler.class)
public class LocationControllerTest extends BaseApplicationTest {

  private static Map<String, String[]> headerMap;
  private static final String LOCATION_NAME = "Laddakh";
  private static final String LOCATION_CODE = "LOC_01";
  private static final String LOCATION_TYPE = "State";
  private static final String LOCATION_ID = "123";
  private static final String CREATE_LOCATION_URL = "/v1/location/create";
  private static final String UPDATE_LOCATION_URL = "/v1/location/update";
  private static final String DELETE_LOCATION_URL = "/v1/location/delete";
  private static final String SEARCH_LOCATION_URL = "/v1/location/search";
  private static final String PARENT_ID = "1a234bc5-dee6-78f9-01g2-h3ij456k7890";


  @Before
  public void before() {
    setup(DummyActor.class);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(
        HeaderParam.X_Authenticated_User_Token.getName(),
        new String[] {"Authenticated user token"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});
  }

  @Test
  public void testCreateLocation() {
    Map userAuthentication = new HashMap<String, String>();
    userAuthentication.put(JsonKey.USER_ID, "uuiuhcf784508 8y8c79-fhh");
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject())).thenReturn(userAuthentication);
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, LOCATION_NAME);
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    locationData.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testCreateLocationWithoutType() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, LOCATION_NAME);
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testCreateLocationWithoutName() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.TYPE, LOCATION_TYPE);
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testCreateLocationWithoutCode() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.TYPE, LOCATION_TYPE);
    locationData.put(JsonKey.NAME, LOCATION_NAME);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.NAME, LOCATION_NAME);
    requestBody.put(JsonKey.CODE, LOCATION_CODE);
    requestBody.put(JsonKey.ID, LOCATION_ID);

    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateLocationWithoutName() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.CODE, LOCATION_CODE);
    requestBody.put(JsonKey.ID, LOCATION_ID);

    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateLocationWithoutCode() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.NAME, LOCATION_NAME);
    requestBody.put(JsonKey.ID, LOCATION_ID);

    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateLocationWithoutID() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.NAME, LOCATION_NAME);
    requestBody.put(JsonKey.CODE, LOCATION_CODE);


    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }
  @Test
  public void testUpdateLocationWithType() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.ID, LOCATION_ID);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }



  @Test
  public void testDeleteLocation() {
    RequestBuilder req =
        new RequestBuilder().uri(DELETE_LOCATION_URL + "/" + LOCATION_ID).method("DELETE");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSearchLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.NAME, LOCATION_NAME);
    filters.put(JsonKey.CODE, LOCATION_CODE);
    filters.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(SEARCH_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }


  @Test
  public void testSearchLocationWithoutName() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.CODE, LOCATION_CODE);
    filters.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(SEARCH_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSearchLocationWIthoutCode() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.NAME, LOCATION_NAME);
    filters.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(SEARCH_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }


  @Test
  public void testSearchLocationWithoutType() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.NAME, LOCATION_NAME);
    filters.put(JsonKey.CODE, LOCATION_CODE);
    requestBody.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(SEARCH_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSearchLocationWithoutNameAndCode() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(SEARCH_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }
  @Test
  public void testCreateLocationWithValidParentId() {

    Map userAuthentication = new HashMap<String, String>();
    userAuthentication = new HashMap<String, String>();
    userAuthentication.put(JsonKey.USER_ID, "uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME,LOCATION_NAME);
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    locationData.put(JsonKey.TYPE, LOCATION_TYPE);
    locationData.put(JsonKey.PARENT_ID,PARENT_ID);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
            new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }
}


}
