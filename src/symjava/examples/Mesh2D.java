package symjava.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import symjava.symbolic.Domain;
import symjava.symbolic.Domain2D;
import symjava.symbolic.Expr;

/**
 * Create a mesh from two files generated by Triangle
 * (http://www.cs.cmu.edu/~quake/triangle.html)
 * 
 * .node files: First line: <# of vertices> <dimension (must be 2)> <# of
 * attributes> <# of boundary markers (0 or 1)> Remaining lines: <vertex #> <x>
 * <y> [attributes] [boundary marker]
 * 
 * .ele files: First line: <# of triangles> <nodes per triangle> <# of
 * attributes> Remaining lines: <triangle #> <node> <node> <node> ...
 * [attributes]
 * 
 */
public class Mesh2D extends Domain2D {
	public List<Node> nodes = new ArrayList<Node>();
	public List<Domain> eles = new ArrayList<Domain>();

	public Mesh2D(String label, Expr... coordVars) {
		super(label, coordVars);
	}

	public List<Domain> getSubDomains() {
		return eles;
	}

	/**
	 * Read a mesh with Triangle format
	 * 
	 * @param nodeFile
	 * @param eleFile
	 */
	public void readTriangleMesh(String nodeFile, String eleFile) {
		this.readNodes(nodeFile);
		this.readElements(eleFile);
	}

	public void readNodes(String nodeFile) {
		FileInputStream in;
		try {
			in = new FileInputStream(nodeFile);
			InputStreamReader reader = new InputStreamReader(in, "UTF-8");
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			int nNode = 0;
			str = br.readLine();
			String[] line = str.trim().split("(\\s)+");
			nNode = Integer.valueOf(line[0]);

			while ((str = br.readLine()) != null) {
				// System.out.println(str);
				if (str.startsWith("#"))
					continue;
				line = str.trim().split("(\\s)+");
				int index = Integer.valueOf(line[0]);
				double x = Double.valueOf(line[1]);
				double y = Double.valueOf(line[2]);
				int marker = Integer.valueOf(line[3]);
				Node node = new Node(x, y);
				node.setIndex(index);
				node.setType(marker);
				nodes.add(node);
			}
			br.close();
			in.close();
			if (nNode != nodes.size()) {
				throw new RuntimeException("Number of nodes is not correct!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readElements(String eleFile) {
		FileInputStream in;
		try {
			in = new FileInputStream(eleFile);
			InputStreamReader reader = new InputStreamReader(in, "UTF-8");
			BufferedReader br = new BufferedReader(reader);
			String str = null;
			int nEle = 0;
			str = br.readLine();
			String[] line = str.trim().split("(\\s)+");
			nEle = Integer.valueOf(line[0]);

			while ((str = br.readLine()) != null) {
				// System.out.println(str);
				if (str.startsWith("#"))
					continue;
				line = str.trim().split("(\\s)+");
				int index = Integer.valueOf(line[0]);
				int n1 = Integer.valueOf(line[1]);
				int n2 = Integer.valueOf(line[2]);
				int n3 = Integer.valueOf(line[3]);
				Element e = new Element(String.format("E%d", index),
						this.coordVars);
				e.setIndex(index);
				e.setNodes(nodes.get(n1 - 1), nodes.get(n2 - 1),
						nodes.get(n3 - 1));
				eles.add(e);
			}
			br.close();
			in.close();
			if (nEle != eles.size()) {
				throw new RuntimeException("Number of elements is not correct!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeTechplot(String fileName, double[][] vec) {
		FileOutputStream out;
		try {
			File file = new File(fileName);
			out = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
			PrintWriter br = new PrintWriter(writer);
			int nNode = nodes.size();
			int nElement = eles.size();
			int nMaxNodes = 3;
			String[] VNs = {"U", "V", "W", "U4", "U5", "U6", "U7", "U8", "U9" };
			int nColumn = vec[0].length;
			if (nMaxNodes == 3) {
				StringBuilder sb = new StringBuilder();
				sb.append("VARIABLES=\"X\",\"Y\"");
				for (int ui = 0; ui < nColumn; ui++)
					sb.append(String.format(",\"%s\"", VNs[ui]));
				br.println(sb.toString());

				if (nMaxNodes == 3)
					br.println(String.format(
							"ZONE F=FEPOINT ET=TRIANGLE N=%d E=%d", nNode,
							nElement));
				else if (nMaxNodes == 6)
					br.println(String.format(
							"ZONE F=FEPOINT ET=TRIANGLE N=%d E=%d", nNode,
							4 * nElement));

				for (int i = 0; i < nNode; i++) {
					Node node = nodes.get(i);
					sb.delete(0, sb.length());
					sb.append("   ");
					for (int ui = 0; ui < nColumn; ui++)
						sb.append(String.format("%f    ", vec[i][ui]));
					br.println(String.format("%f    %f    %s",
							node.coords[0], node.coords[1], sb.toString()));
				}
				for (int i = 0; i < nElement; i++) {
					Element e = (Element) eles.get(i);
					if (e.nodes.size() == 3) {
						br.println(String.format("%d    %d    %d",
								e.nodes.get(0).index, e.nodes.get(1).index,
								e.nodes.get(2).index));
					} else if (e.nodes.size() == 6) {
						br.println(String.format("%d    %d    %d",
								e.nodes.get(0).index, e.nodes.get(3).index,
								e.nodes.get(5).index));
						br.println(String.format("%d    %d    %d",
								e.nodes.get(1).index, e.nodes.get(4).index,
								e.nodes.get(3).index));
						br.println(String.format("%d    %d    %d",
								e.nodes.get(2).index, e.nodes.get(5).index,
								e.nodes.get(4).index));
						br.println(String.format("%d    %d    %d",
								e.nodes.get(3).index, e.nodes.get(4).index,
								e.nodes.get(5).index));
					} else {
						System.out.println("Error: TRIANGLE nodes number="
								+ e.nodes.size());
					}

				}
			} else if (nMaxNodes == 4) {
				StringBuilder sb = new StringBuilder();
				sb.append("VARIABLES=\"X\",\"Y\"");
				for (int ui = 0; ui < nColumn; ui++)
					sb.append(String.format(",\"%s\"", VNs[ui]));
				br.println(sb.toString());

				if (nMaxNodes == 4)
					br.println(String.format(
							"ZONE F=FEPOINT ET=QUADRILATERAL N=%d E=%d", nNode,
							nElement));
				else if (nMaxNodes == 8)
					br.println(String.format(
							"ZONE F=FEPOINT ET=QUADRILATERAL N=%d E=%d", nNode,
							5 * nElement));

				for (int i = 0; i < nNode; i++) {
					Node node = nodes.get(i);
					sb.delete(0, sb.length());
					sb.append("   ");
					for (int ui = 0; ui < nColumn; ui++)
						sb.append(String.format("%f    ", vec[i][ui]));
					br.println(String.format("%f    %f    %s",
							node.coords[0], node.coords[1], sb.toString()));
				}
				for (int i = 0; i < nElement; i++) {
					Element e = (Element) eles.get(i);
					if (e.nodes.size() == 4) {
						br.println(String.format("%d    %d    %d    %d",
								e.nodes.get(0).index, e.nodes.get(1).index,
								e.nodes.get(2).index, e.nodes.get(3).index));
					} else if (e.nodes.size() == 8) {
						br.println(String.format("%d    %d    %d    %d",
								e.nodes.get(0).index, e.nodes.get(4).index,
								e.nodes.get(7).index, e.nodes.get(0).index));
						br.println(String.format("%d    %d    %d    %d",
								e.nodes.get(1).index, e.nodes.get(5).index,
								e.nodes.get(4).index, e.nodes.get(1).index));
						br.println(String.format("%d    %d    %d    %d",
								e.nodes.get(2).index, e.nodes.get(6).index,
								e.nodes.get(7).index, e.nodes.get(2).index));
						br.println(String.format("%d    %d    %d    %d",
								e.nodes.get(3).index, e.nodes.get(7).index,
								e.nodes.get(6).index, e.nodes.get(3).index));
						br.println(String.format("%d    %d    %d    %d",
								e.nodes.get(4).index, e.nodes.get(5).index,
								e.nodes.get(6).index, e.nodes.get(7).index));
					} else if (e.nodes.size() == 3) {
						br.println(String.format("%d    %d    %d    %d",
								e.nodes.get(0).index, e.nodes.get(1).index,
								e.nodes.get(2).index, e.nodes.get(0).index));
					} else {
						System.out.println("Error: QUADRILATERAL nodes number="
								+ e.nodes.size());
					}
				}
			}
			br.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void readGridGenMesh(String file) {
		FileInputStream in;
		try {
			in = new FileInputStream(file);

			InputStreamReader reader = new InputStreamReader(in,"UTF-8");
			BufferedReader br = new BufferedReader(reader);
	
			String str = null;
			int nNode = 0;
			int nElement = 0;
			while((str = br.readLine()) != null){
				//System.out.println(str);
				if(str.startsWith("#")) continue;
				String[] line = str.trim().split("(\\s)+");
				if(nNode == 0) {
					nNode = Integer.valueOf(line[0]);
					nElement = Integer.valueOf(line[1]);
				} else {
					if(nodes.size() < nNode) {
						int index = Integer.valueOf(line[0]);
						double x = Double.valueOf(line[1]);
						double y = Double.valueOf(line[2]);
						Node node = new Node(x, y);
						node.setIndex(index);
						nodes.add(node);
					} else if(eles.size() < nElement) {
						String type = line[2];
						Element ele = new Element(String.format("E%d",eles.size()), this.coordVars);
						if(type.equalsIgnoreCase("tri")) {
							ele.setNodes(
									nodes.get(Integer.valueOf(line[3])-1),
									nodes.get(Integer.valueOf(line[4])-1),
									nodes.get(Integer.valueOf(line[5])-1)
									);
							eles.add(ele);
						} else if(type.equalsIgnoreCase("quad")) {
							ele.setNodes(
									nodes.get(Integer.valueOf(line[3])-1),
									nodes.get(Integer.valueOf(line[4])-1),
									nodes.get(Integer.valueOf(line[5])-1),
									nodes.get(Integer.valueOf(line[6])-1)
									);
							eles.add(ele);
						}
					}
				}
			}
			br.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}