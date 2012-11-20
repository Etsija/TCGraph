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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class TCConverter {

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

	private static class PathNode {
		
		public String name;
		public int x, y, z;
		public String world;
		public ArrayList<PathConnection> connections = new ArrayList<PathConnection>();
		public String getLoc() {
			//return world + "_" + x + "_" + y + "_" + z;
			return "(" + world + "," + x + "," + y + "," + z + ")";
		}
	}
	
	public static void main(String[] args) {
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select the destinations.dat file to open");
		
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			File sourceFile = fileChooser.getSelectedFile();
			String sourceBase = sourceFile.getName().split("\\.")[0];
			
			fileChooser.setDialogTitle("Select the file to save the converted text to");
			fileChooser.setSelectedFile(new File(sourceFile.getParentFile(), sourceBase + ".txt"));
			
			if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
				File destFile = fileChooser.getSelectedFile();
			
				try {
					DataInputStream stream = new DataInputStream(new InflaterInputStream(new FileInputStream(sourceFile)));
					PathNode[] nodes;
				
					try {
						nodes = new PathNode[stream.readInt()];
						HashMap<String, PathNode> nodesMap = new HashMap<String, PathNode>(nodes.length);
						
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
								//nodes[i].name = nodes[i].getLoc();
								nodes[i].name = "JN" + Integer.toString(j);
								j++;
							}
							nodesMap.put(nodes[i].name, nodes[i]);
						}

						// Read all connections
						for (PathNode node : nodes) {
							PathConnection conn;
							int ncount = stream.readInt();
							
							for (int i = 0; i < ncount; i++) {
								conn = new PathConnection(stream, nodes[stream.readInt()]);
								node.connections.add(conn);
							}
							
							ncount = stream.readInt();
							
							for (int i = 0; i < ncount; i++) {
								conn = new PathConnection(stream, nodes[stream.readInt()]);
								// These are very far away connections and should be ignored
							}
						}
						
					} finally {
						stream.close();
					}
					
					// Find and mark the two-way connections from the map
					for (PathNode node : nodes) {
						for (PathConnection sourceConn : node.connections) {
							PathNode destNode = sourceConn.destination;
							for (PathConnection destConn : destNode.connections) {
								if (destConn.destination == node) {
									if (sourceConn.shouldBeListed) {
										sourceConn.isTwoway = true;
										destConn.shouldBeListed = false;
									}
								}
							}
						}
					}
					
					for (PathNode node : nodes) {
						for (PathConnection conn : node.connections) {
							if (conn.shouldBeListed)
								System.out.println(node.name + " -> " + conn.destination.name 
										+ ", istwoway=" + conn.isTwoway);
						}
					}
					
					// Start writing this info
					BufferedWriter  writer = new BufferedWriter(new FileWriter(destFile));
					String destBase = destFile.getAbsolutePath().split("\\.")[0];
					BufferedWriter dwriter = new BufferedWriter(new FileWriter(destBase + ".dot"));

					try {
						//dwriter.write("digraph PathFinding {");
						dwriter.write("graph PathFinding {");
						dwriter.newLine();
						//dwriter.write("    size=\"20\" splines=ortho;");
						
						dwriter.newLine();
						dwriter.write("    node [style=filled, shape=box, fontname=Helvetica, fontsize=14];");
						dwriter.newLine();
						dwriter.newLine();
						for (PathNode node : nodes) {
							writer.write("name: ");
							writer.write(node.name);
							writer.newLine();
							
							dwriter.write("    " + node.name + " [");
							//dwriter.write("pos=\"" + node.x + "," + node.z + "!\"");
							dwriter.write("pos=\"" + node.x + "," + node.z + "\"");
							if (!node.name.startsWith("JN")) {
								dwriter.write(", fillcolor=yellow");
							} else {
								dwriter.write(", shape=point");
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
										dwriter.write("    " + node.name + " -- " + conn.destination.name + " [" 
											      //+ "len=" + conn.distance + ", "
											      //+ "tailport=" + conn.direction + ", "
												  //+ "dir=\"both\", "
											      + "label=" + conn.distance + "];");
									} else {
											dwriter.write("    " + node.name + " -- " + conn.destination.name + " ["
										          //+ "len=" + conn.distance + ", "
										          //+ "tailport=" + conn.direction + ", "
												  + "dir=\"forward\", "
										          + "label=" + conn.distance + ", "
										          + "color=red];");
									}
									dwriter.newLine();
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
						dwriter.write("}");
						dwriter.newLine();
					} finally {
						writer.close();
						dwriter.close();
					}
					JOptionPane.showMessageDialog(null, "The operation was successful!", "Conversion finished", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error while loading file", JOptionPane.ERROR_MESSAGE);
				}
				System.exit(0);
				return;
			}
		}
		JOptionPane.showMessageDialog(null, "Cancelled, the program will now close.", "Aborted", JOptionPane.INFORMATION_MESSAGE);
		System.exit(0);
	}
}
