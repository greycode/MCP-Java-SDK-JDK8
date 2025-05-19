/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycompany.aigw.mcp.client.autoconfigure;

import com.mycompany.aigw.spec.McpClientTransport;

import java.util.Objects;

/**
 * A named MCP client transport. Usually created by the transport auto-configurations, but
 * you can also create them manually.
 *
 * @author Christian Tzolov
 * @since 1.0.0
 */
public class NamedClientMcpTransport {
    
    private final String name;
    private final McpClientTransport transport;
    
    /**
     * Constructor for a named MCP client transport.
     * @param name the name of the transport. Usually the name of the server connection.
     * @param transport the MCP client transport.
     */
    public NamedClientMcpTransport(String name, McpClientTransport transport) {
        this.name = name;
        this.transport = transport;
    }
    
    /**
     * Get the name of the transport.
     * @return the name
     */
    public String name() {
        return name;
    }
    
    /**
     * Get the MCP client transport.
     * @return the transport
     */
    public McpClientTransport transport() {
        return transport;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedClientMcpTransport that = (NamedClientMcpTransport) o;
        return Objects.equals(name, that.name) && 
               Objects.equals(transport, that.transport);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, transport);
    }
    
    @Override
    public String toString() {
        return "NamedClientMcpTransport[name=" + name + ", transport=" + transport + "]";
    }
} 