package org.jmc.models;

import java.util.HashMap;

import org.jmc.geom.Transform;
import org.jmc.geom.Vertex;
import org.jmc.threading.ChunkProcessor;
import org.jmc.threading.ThreadChunkDeligate;


/**
 * Generic model for blocks rendered as 2 crossed polygons, like saplings.
 */
public class CoralFan extends BlockModel
{

	@Override
	public void addModel(ChunkProcessor obj, ThreadChunkDeligate chunks, int x, int y, int z, HashMap<String, String> data, int biome)
	{
		Transform move = new Transform();
		Transform rotate = new Transform();
		Transform rt;
		move.translate(x, y, z);
		
		// If it's not a wall fan 
		if (!data.containsKey("facing")) 
		{
			Vertex[] vertices = new Vertex[4];
			vertices[0] = new Vertex(0.5f,-0.5f,0.0f);
			vertices[1] = new Vertex(-0.5f,-0.5f,0.0f);	
			vertices[2] = new Vertex(-0.5f,-0.05f,0.8f);
			vertices[3] = new Vertex(0.5f,-0.05f,0.8f);
			obj.addFace(vertices, null, move, materials.get(data,biome)[0]);
		
			vertices[0] = new Vertex(-0.5f,-0.5f,0.0f);
			vertices[1] = new Vertex(0.5f,-0.5f,0.0f);
			vertices[2] = new Vertex(0.5f,-0.05f,-0.8f);
			vertices[3] = new Vertex(-0.5f,-0.05f,-0.8f);
			obj.addFace(vertices, null, move, materials.get(data,biome)[0]);

			vertices[0] = new Vertex(0.0f,-0.5f,0.5f);
			vertices[1] = new Vertex(0.0f,-0.5f,-0.5f);	
			vertices[2] = new Vertex(-0.8f,-0.05f,-0.5f);		
			vertices[3] = new Vertex(-0.8f,-0.05f,0.5f);
			obj.addFace(vertices, null, move, materials.get(data,biome)[0]);	
			
			vertices[0] = new Vertex(0.0f,-0.5f,-0.5f);
			vertices[1] = new Vertex(0.0f,-0.5f,0.5f);	
			vertices[2] = new Vertex(0.8f,-0.05f,0.5f);		
			vertices[3] = new Vertex(0.8f,-0.05f,-0.5f);
			obj.addFace(vertices, null, move, materials.get(data,biome)[0]);		
		}
		else // It is a wall fan
		{
			switch (data.get("facing"))
			{
				case "north":
					rotate.rotate(0, 0, 0);
					break;
				case "east":
					rotate.rotate(0, 90, 0);
					break;
				case "south":
					rotate.rotate(0, 180, 0);
					break;
				case "west":
					rotate.rotate(0, -90, 0);
					break;
				default:
					
					break;
			}
			rt = move.multiply(rotate);
			
			Vertex[] vertices = new Vertex[4];
			vertices[0] = new Vertex(-0.5f, -0.04f, 0.5f);
			vertices[1] = new Vertex( 0.5f, -0.04f, 0.5f);
			vertices[2] = new Vertex( 0.5f,  0.28f, -0.5f);
			vertices[3] = new Vertex(-0.5f,  0.28f, -0.5f);
			obj.addFace(vertices, null, rt, materials.get(data,biome)[0]);			
			
			vertices[0] = new Vertex(-0.5f, 0.04f, 0.5f);
			vertices[1] = new Vertex( 0.5f, 0.04f, 0.5f);
			vertices[2] = new Vertex( 0.5f,  -0.28f, -0.5f);
			vertices[3] = new Vertex(-0.5f,  -0.28f, -0.5f);
			obj.addFace(vertices, null, rt, materials.get(data,biome)[0]);				
		}	
	}

}
