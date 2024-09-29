package org.matsim.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.yaml.snakeyaml.reader.ReaderException;


import java.io.*;
import java.util.*;

@SuppressWarnings({"all"})
/**
 * Test
 */
public final class GenerateSimulationNetwork {
	static final String sharePath = "G:\\TU Stuff\\MatSim\\HA2\\LinkList.txt";

	private GenerateSimulationNetwork() {
	}

	public static void main(String[] args) {
		Network network = NetworkUtils.readNetwork("G:\\TU Stuff\\MatSim\\HA2\\berlin-v6.3-network-with-pt.xml");
		HashMap<Integer, String> linkList = readFile(sharePath);
		for (var link : linkList.entrySet()) {
			String linkID = link.getValue();
			for (Link linkZuBearbeiten : network.getLinks().values()) {
				linkEditor(linkZuBearbeiten, linkID);
			}
		}
		NetworkUtils.writeNetwork(network, "G:\\TU Stuff\\MatSim\\HA2\\berlin-v6.3-network-with-pt-bearbeitet.xml");
	}

	private static void linkEditor(Link link, String linkId) {
		if (link.getId().equals(Id.createLinkId(linkId))) {
			Set<String> modes = new HashSet<String>();
			modes.add("bike");
			modes.add("walk");
			link.setAllowedModes(modes);
			link.setFreespeed(6);
		}
	}


	private static HashMap<Integer, String> readFile(String fileLocation) {
		HashMap<Integer, String> map = new HashMap<>();
		try {
			File filename = new File(fileLocation);
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String info = null;
			int counter = 0;
			while ((info = reader.readLine()) != null) {
				map.put(counter,info);
				counter++;
			}
		} catch (ReaderException e) {
			System.out.println(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return map;
	}

}
