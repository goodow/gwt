/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.requestfactory.server;

import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.impl.JsonRequestDataUtil;
import com.google.gwt.valuestore.server.SimpleFoo;
import com.google.gwt.valuestore.shared.SimpleEnum;
import com.google.gwt.valuestore.shared.SimpleFooRecord;
import com.google.gwt.valuestore.shared.WriteOperation;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Tests for {@link JsonRequestProcessor} .
 */
public class JsonRequestProcessorTest extends TestCase {

  enum Foo {
    BAR, BAZ
  }

  private JsonRequestProcessor requestProcessor;;

  public void setUp() {
    requestProcessor = new JsonRequestProcessor();
    requestProcessor.setOperationRegistry(new ReflectionBasedOperationRegistry(
        new DefaultSecurityProvider()));
  }

  public void testDecodeParameterValue() {
    // primitives
    assertTypeAndValueEquals(String.class, "Hello", "Hello");
    assertTypeAndValueEquals(Integer.class, 1234, "1234");
    assertTypeAndValueEquals(Byte.class, (byte) 100, "100");
    assertTypeAndValueEquals(Short.class, (short) 12345, "12345");
    assertTypeAndValueEquals(Float.class, 1.234f, "1.234");
    assertTypeAndValueEquals(Double.class, 1.234567, "1.234567");
    assertTypeAndValueEquals(Long.class, 1234L, "1234");
    assertTypeAndValueEquals(Boolean.class, true, "true");
    assertTypeAndValueEquals(Boolean.class, false, "false");

    // dates
    Date now = new Date();
    assertTypeAndValueEquals(Date.class, now, "" + now.getTime());
    // bigdecimal and biginteger
    BigDecimal dec = new BigDecimal("10").pow(100);
    assertTypeAndValueEquals(BigDecimal.class, dec, dec.toPlainString());
    assertTypeAndValueEquals(BigInteger.class, dec.toBigInteger(),
        dec.toBigInteger().toString());
    // enums
    assertTypeAndValueEquals(Foo.class, Foo.BAR, "" + Foo.BAR.ordinal());
  }

  public void testEncodePropertyValue() {
    assertEncodedType(String.class, "Hello");
    // primitive numbers become doubles
    assertEncodedType(Double.class, (byte) 10);
    assertEncodedType(Double.class, (short) 1234);
    assertEncodedType(Double.class, 123.4f);
    assertEncodedType(Double.class, 1234.0);
    assertEncodedType(Double.class, 1234);
    // longs, big nums become strings
    assertEncodedType(String.class, 1234L);
    assertEncodedType(String.class, new BigDecimal(1));
    assertEncodedType(String.class, new BigInteger("1"));
    assertEncodedType(String.class, new Date());
    assertEncodedType(Double.class, Foo.BAR);
    assertEncodedType(Boolean.class, true);
    assertEncodedType(Boolean.class, false);
  }

  public void testEndToEnd() {
    com.google.gwt.valuestore.server.SimpleFoo.reset();
    try {
      // fetch object
      JSONObject foo = (JSONObject) requestProcessor.processJsonRequest("{ \""
          + JsonRequestDataUtil.OPERATION_TOKEN + "\": \""
          + com.google.gwt.valuestore.shared.SimpleFooRequest.class.getName()
          + ReflectionBasedOperationRegistry.SCOPE_SEPARATOR
          + "findSimpleFooById\", " + "\"" + JsonRequestDataUtil.PARAM_TOKEN
          + "0\": \"999\" }");
      assertEquals(foo.getInt("id"), 999);
      assertEquals(foo.getInt("intId"), 42);
      assertEquals(foo.getString("userName"), "GWT");
      assertEquals(foo.getLong("longField"), 8L);
      assertEquals(foo.getInt("enumField"), 0);
      assertEquals(foo.getInt("version"), 1);
      assertEquals(foo.getBoolean("boolField"), true);
      assertTrue(foo.has("created"));

      // modify fields and sync
      foo.put("intId", 45);
      foo.put("userName", "JSC");
      foo.put("longField", "" + 9L);
      foo.put("enumField", SimpleEnum.BAR.ordinal());
      foo.put("boolField", false);
      Date now = new Date();
      foo.put("created", "" + now.getTime());
      JSONObject recordWithSchema = new JSONObject();
      recordWithSchema.put(SimpleFooRecord.class.getName(), foo);
      JSONArray arr = new JSONArray();
      arr.put(recordWithSchema);
      JSONObject operation = new JSONObject();
      operation.put(WriteOperation.UPDATE.toString(), arr);
      JSONObject sync = new JSONObject();
      sync.put(JsonRequestDataUtil.OPERATION_TOKEN, RequestFactory.SYNC);
      sync.put(JsonRequestDataUtil.CONTENT_TOKEN, operation.toString());
      JSONObject result = (JSONObject) requestProcessor.processJsonRequest(sync.toString());

      // check modified fields and no violations
      SimpleFoo fooResult = SimpleFoo.getSingleton();
      assertFalse(result.getJSONArray("UPDATE").getJSONObject(0).has(
          "violations"));
      assertEquals((int) 45, (int) fooResult.getIntId());
      assertEquals("JSC", fooResult.getUserName());
      assertEquals(now, fooResult.getCreated());
      assertEquals(9L, (long) fooResult.getLongField());
      assertEquals(com.google.gwt.valuestore.shared.SimpleEnum.BAR,
          fooResult.getEnumField());
      assertEquals(false, (boolean)fooResult.getBoolField());
    } catch (Exception e) {
      fail(e.toString());
    }
  }

  private void assertEncodedType(Class<?> expected, Object value) {
    assertEquals(expected,
        requestProcessor.encodePropertyValue(value).getClass());
  }

  private <T> void assertTypeAndValueEquals(Class<T> expectedType,
      T expectedValue, String paramValue) {
    Object val = requestProcessor.decodeParameterValue(expectedType, paramValue);
    assertEquals(expectedType, val.getClass());
    assertEquals(expectedValue, val);
  }
}