/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.client.idgenerator.impl;

import java.util.concurrent.CompletableFuture;

import io.atomix.api.counter.CounterId;
import io.atomix.api.protocol.DistributedLogProtocol;
import io.atomix.api.protocol.MultiRaftProtocol;
import io.atomix.client.PrimitiveManagementService;
import io.atomix.client.counter.impl.DefaultAsyncAtomicCounter;
import io.atomix.client.idgenerator.AsyncAtomicIdGenerator;
import io.atomix.client.idgenerator.AtomicIdGenerator;
import io.atomix.client.idgenerator.AtomicIdGeneratorBuilder;
import io.atomix.client.idgenerator.AtomicIdGeneratorConfig;

/**
 * Default implementation of AtomicIdGeneratorBuilder.
 */
public class DelegatingAtomicIdGeneratorBuilder extends AtomicIdGeneratorBuilder {
  public DelegatingAtomicIdGeneratorBuilder(String name, AtomicIdGeneratorConfig config, PrimitiveManagementService managementService) {
    super(name, config, managementService);
  }

  private CounterId createCounterId() {
    CounterId.Builder builder = CounterId.newBuilder().setName(name);
    protocol = protocol();
    if (protocol instanceof io.atomix.protocols.raft.MultiRaftProtocol) {
      builder.setRaft(MultiRaftProtocol.newBuilder()
          .setGroup(((io.atomix.protocols.raft.MultiRaftProtocol) protocol).group())
          .build());
    } else if (protocol instanceof io.atomix.protocols.log.DistributedLogProtocol) {
      builder.setLog(DistributedLogProtocol.newBuilder()
          .setGroup(((io.atomix.protocols.log.DistributedLogProtocol) protocol).group())
          .setPartitions(((io.atomix.protocols.log.DistributedLogProtocol) protocol).config().getPartitions())
          .setReplicationFactor(((io.atomix.protocols.log.DistributedLogProtocol) protocol).config().getReplicationFactor())
          .build());
    }
    return builder.build();
  }

  @Override
  public CompletableFuture<AtomicIdGenerator> buildAsync() {
    return new DefaultAsyncAtomicCounter(createCounterId(), managementService.getChannelFactory(), managementService)
        .connect()
        .thenApply(DelegatingAtomicIdGenerator::new)
        .thenApply(AsyncAtomicIdGenerator::sync);
  }
}