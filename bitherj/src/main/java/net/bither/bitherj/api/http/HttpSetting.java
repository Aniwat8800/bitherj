/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.bitherj.api.http;

public class HttpSetting {
    /**
     * HTTP_CONNECTION_TIMEOUT: Set the timeout in milliseconds until a
     * connection is established. The default value is zero, that means the
     * timeout is not used.
     */
    public static final int HTTP_CONNECTION_TIMEOUT = 1000 * 1000;
    /**
     * HTTP_SO_TIMEOUT: Set the default socket timeout (SO_TIMEOUT). in
     * milliseconds which is the timeout for waiting for data.
     */
    public static final int HTTP_SO_TIMEOUT = 1400 * 1000;

    public final static String SPECIAL_TYPE = "special_type";

    // enum
    public enum HttpType {
        BitherApi, OtherApi, GetBitherCookie
    }

    public static final String PASSWORD = "password";
    public static final String SIGNATURE = "signature";

    public static final String PUB_HOT = "pub_hot";
    public static final String PUB_COLD = "pub_cold";
    public static final String START = "start";
    public static final String END = "end";
}
















