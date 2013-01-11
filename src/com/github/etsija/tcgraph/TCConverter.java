package com.github.etsija.tcgraph;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.InflaterInputStream;

public class TCConverter {
	
	// This class represents an edge in the routing graph
	private static class PathConnection {	
		public PathConnection(DataInputStream stream, PathNode dest) throws IOException {
			this.distance = stream.readInt();
			switch (stream.readByte()) {
				case 1 : this.direction = "e"; break;
				case 2 : this.direction = "s"; break;
				case 3 : this.direction = "w"; break;
				default : this.direction = "n"; break;
			}
			this.destination = dest;
		}
		public PathNode destination;
		public int distance;
		public String direction;
		public boolean isTwoway = false;
		public boolean shouldBeListed = true;
	}

	
	// This class represents a node in the routing graph
	private static class PathNode {
		public String name;
		public int x, y, z;
		public String world;
		public ArrayList<PathConnection> connections = new ArrayList<PathConnection>();
		public String getLoc() {
			return "(" + world + "," + x + "," + y + "," + z + ")";
		}
	}
	
	
	// Find and mark the two-way connections
	private static int markTwoWay(PathNode[] nodes) {
		int removedNodes = 0;
		for (PathNode node : nodes) {
			for (PathConnection sourceConn : node.connections) {
				PathNode destNode = sourceConn.destination;
				for (PathConnection destConn : destNode.connections) {
					if (destConn.destination == node) {
						if (sourceConn.shouldBeListed) {
							sourceConn.isTwoway = true;
							destConn.shouldBeListed = false;
							removedNodes++;
						}
					}
				}
			}
		}
		return removedNodes;
	}
	
	
	// Main
	public static void main(String[] args) {
		
		int connBeforeRemoval = 0;
		int connAfterRemoval = 0;
		int nNodes = 0;
		int nDestinations = 0;
		boolean labelIntersections = false;
		boolean debug = false;
		File sourceFile = null;
		File textFile = null;
		File dotFile = null;
		
		if (args.length < 3) {

			System.out.println("Usage: <destinations.dat> <destinations.txt> <destinations.dot> -label -debug");
			System.out.println("  <destinations.dat> = (mandatory) the TrainCarts input file");
			System.out.println("  <destinations.txt> = (mandatory) informational output file");
			System.out.println("  <destinations.dot> = (mandatory) dotfile to input to DOT tool");
			System.out.println("              -label = (optional) label intersections with \"JN\" ");
			System.out.println("              -debug = (optional) print out some debug info");
			System.out.println();
			System.out.println("Note: args can be in any order as long as the filenames include");
			System.out.println(".dat, .txt, .dot, respectively.  For example this is still valid:");
			System.out.println("> tcgraph destinations.txt -debug destinations.dot -label destinations.dat");
			System.exit(0);

		} else {
		
			// Handle arguments
			for (String s: args) {
				if (s.contains(".dat"))
					sourceFile = new File(s);
				else if (s.contains(".txt"))
					textFile = new File(s);
				else if (s.contains(".dot"))
					dotFile = new File(s);
				else if (s.equals("-label"))
					labelIntersections = true;
				else if (s.equals("-debug"))
					debug = true;			
			}
			
			try {
				DataInputStream stream = new DataInputStream(new InflaterInputStream(new FileInputStream(sourceFile)));
				PathNode[] nodes;
				
				try {
					nodes = new PathNode[stream.readInt()];
					HashMap<String, PathNode> nodesMap = new HashMap<String, PathNode>(nodes.length);
					nNodes = nodes.length;
					
					// Read all node information
					int j = 1;
					for (int i = 0; i < nodes.length; i++) {
						nodes[i] = new PathNode();
						nodes[i].name = stream.readUTF();
						nodes[i].world = stream.readUTF();
						nodes[i].x = stream.readInt();
						nodes[i].y = stream.readInt();
						nodes[i].z = stream.readInt();
						
						if (nodes[i].name.isEmpty()) {
							nodes[i].name = "JN" + Integer.toString(j);
							j++;
						}
						nodesMap.put(nodes[i].name, nodes[i]);
					}

					// Read all connections
					for (PathNode node : nodes) {
							
						PathConnection conn;
						int ncount = stream.readInt();
						connBeforeRemoval += ncount;
						
						if (debug) {
							System.out.println(node.name + " conn: " + ncount);
						}
							
						for (int i = 0; i < ncount; i++) {
							conn = new PathConnection(stream, nodes[stream.readInt()]);
							node.connections.add(conn);
						}							
					}
						
				} finally {
					stream.close();
				}

				// Find and mark the two-way connections so they are not drawn twice in the graph
				int removedConn = markTwoWay(nodes);
					
				for (PathNode node : nodes) {
					for (PathConnection conn : node.connections) {
						if (conn.shouldBeListed) {
							if (debug) {
								System.out.println(node.name + " -> " + conn.destination.name 
										+ ", istwoway=" + conn.isTwoway);
							}
							connAfterRemoval++;
						}
					}
				}
					
				// Start writing this info
				BufferedWriter  writer = new BufferedWriter(new FileWriter(textFile));
				BufferedWriter dwriter = new BufferedWriter(new FileWriter(dotFile));

				try {
					dwriter.write("graph PathFinding {");
					dwriter.newLine();
						
					dwriter.newLine();
					dwriter.write("    node [width=0, height=0, margin=0, fontname=Helvetica, fontsize=14];");
					dwriter.newLine();
					dwriter.newLine();
					int totalTracks = 0;
					for (PathNode node : nodes) {
						writer.write("name: ");
						writer.write(node.name);
						writer.newLine();
							
						dwriter.write("    \"" + node.name + "\" [");
						// Flip the z coordinate to make the graphics roughly respect the
						// real directions (n,e,s,w)
						dwriter.write("pos=\"" + node.x + "," + -node.z + "\", ");
							
						// Switches (junctions) represented as points or texts, depending on user's choice
						if (node.name.startsWith("JN")) {
							if (labelIntersections)
								dwriter.write("shape=plaintext, fontsize=10");
							else
								dwriter.write("shape=point, width=0.1, height=0.1");
						// Real destinations represented as yellow boxes
						} else {
							dwriter.write("style=filled, " +
									      "shape=ellipse, " +
									      "fillcolor=yellow, " +
									      "margin=0.08");
							nDestinations++;
						}
						dwriter.write("];");
						dwriter.newLine();
							
						writer.write("position: ");
						writer.write(node.getLoc());
						writer.newLine();
						writer.write("connections (" + node.connections.size() + "):");
						writer.newLine();
							
						for (PathConnection conn : node.connections) {					
							writer.write("- ");
							writer.write(conn.destination.name);
							writer.newLine();
								
							if (conn.shouldBeListed) {
								if (conn.isTwoway) {
									dwriter.write("    \"" + node.name + "\" -- \"" + conn.destination.name + "\" [" 
										      + "label=" + conn.distance + "];");
								} else {
										dwriter.write("    \"" + node.name + "\" -- \"" + conn.destination.name + "\" ["
											  + "dir=\"forward\", "
									          + "label=" + conn.distance + ", "
									          + "color=red];");
								}
								dwriter.newLine();
								totalTracks += conn.distance;
							}
								
							writer.write("    position: ");
							writer.write(conn.destination.getLoc());
							writer.newLine();
							writer.write("    direction: ");
							writer.write(conn.direction);
							writer.newLine();
							writer.write("    distance: ");
							writer.write(Integer.toString(conn.distance));
							writer.newLine();
						}
						writer.newLine();
						dwriter.newLine();
					}
					System.out.println("Total number of nodes in graph: " + nNodes);
					System.out.println("      ...of which destinations: " + nDestinations);
					System.out.println(" Connections in original graph: " + connBeforeRemoval);
					System.out.println("   Two-way connections removed: " + removedConn);
					System.out.println("    Connections in final graph: " + connAfterRemoval);
					System.out.println("        Total number of tracks: " + totalTracks);
					dwriter.write("}");
					dwriter.newLine();
				} finally {
					writer.close();
					dwriter.close();
				}
				System.out.println("The operation was successful!");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.exit(0);
	}
}
