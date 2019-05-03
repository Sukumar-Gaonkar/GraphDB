package com.graphdb;

import java.util.Collection;
import java.util.Map;

import com.graphdb.model.Relation;
import io.atomix.utils.time.Versioned;
import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.graphdb.agent.JsonAgent;
import com.graphdb.model.Graph;
import com.graphdb.model.GraphModelImpl;

import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.primitive.protocol.ProxyProtocol;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;

public class GraphDBClient {

	private final static Logger logger = Logger.getLogger(GraphDBClient.class);

	public static void main(String... args) {

		logger.info("Client starting....");

		String clientAddress;
		if (args == null || args.length == 0) {
			clientAddress = "localhost:8084";
		} else {
			clientAddress = args[0];
		}

		AtomixBuilder atomixBuilder = Atomix.builder().withMemberId("client1").withAddress(clientAddress)
				.withMembershipProvider(BootstrapDiscoveryProvider.builder()
						.withNodes(Node.builder().withId("member1").withAddress("localhost:8080").build(),
								Node.builder().withId("member2").withAddress("localhost:8081").build(),
								Node.builder().withId("member3").withAddress("localhost:8082").build(),
								Node.builder().withId("member4").withAddress("localhost:8083").build()
						)
						.build());

		Atomix atomixAgent = atomixBuilder.build();

		atomixAgent.start().join();

		logger.info("Client started....");

		Graph<String, String> graph = new GraphModelImpl<>(atomixAgent, "multimap");
		ProxyProtocol protocol = MultiRaftProtocol.builder().withReadConsistency(ReadConsistency.LINEARIZABLE).build();
		graph.withProtocol(protocol);
		graph.buildAtomicMultiMap();

		JsonAgent<Map<String, Collection<String>>> jsonAgent = new JsonAgent<>();

		Multimap<String, String> nodeData1 = HashMultimap.create();
		nodeData1.put("1", "abc");
		nodeData1.put("1", "fool");
		nodeData1.put("1", "def");

		String json1 = jsonAgent.toJson(nodeData1.asMap());


		Multimap<String, String> nodeData2 = HashMultimap.create();
		nodeData2.put("2", "abcsd");
		nodeData2.put("2", "foolasd");
		nodeData2.put("2", "defasd");

		String json2 = jsonAgent.toJson(nodeData2.asMap());

		System.out.println("Adding node1");
		graph.addNode("node1", json1);

		System.out.println("Adding node2");
		graph.addNode("node2", json2);

		System.out.println("Adding relation node1 -> node2");
		graph.addRelation("node1", "node2", "normal", "yo", true);

		System.out.println("All Relations");
		for(Map.Entry<Long, Versioned<Relation>> entry: ((GraphModelImpl<String, String>)graph).relationsMap.entrySet()){
			System.out.println(entry.getValue().value());
		}

		System.out.println("graph.getRelations(\"node2\", \"node1\")");
		System.out.println(graph.getRelations("node2", "node1"));

		System.out.println("Adding relation node1 -> node2");
		graph.addRelation("node1", "node2", "normal", "yo", true);

		System.out.println("graph.getRelations(\"node2\", \"node1\")");
		System.out.println(graph.getRelations("node2", "node1").size());

		System.out.println("graph.getOutgoingRelations(\"node2\").size()");
		System.out.println(graph.getOutgoingRelations("node2").size());

		System.out.println("graph.getIncomingRelations(\"node2\").size()");
		System.out.println(graph.getIncomingRelations("node2").size());

		System.out.println("graph.areRelated(\"node1\", \"node2\")");
		System.out.println(graph.areRelated("node1", "node2"));
	}
}
