/*******************************************************************************
 * Copyright (c) 2012
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package org.jmc.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import org.jmc.Chunk;
import org.jmc.ChunkDataBuffer;
import org.jmc.Materials;
import org.jmc.OBJOutputFile;
import org.jmc.Region;
import org.jmc.gui.OBJExportOptions.OffsetType;
import org.jmc.gui.OBJExportOptions.OverwriteAction;
import org.jmc.util.Filesystem;
import org.jmc.util.Log;

/**
 * A thread used and UI for saving an OBJ file.
 * When run it saves the file and shows a small window that shows the progress of
 * saving the file.
 * @author danijel
 *
 */
@SuppressWarnings("serial")
public class OBJExportPanel extends JFrame implements Runnable {

	/**
	 * Path to the world save.
	 */
	File savepath;
	/**
	 * Dimension to load chunks from
	 */
	int dimension;
	/**
	 * Region of the map to be saved.
	 */
	Rectangle bounds;
	/**
	 * Altitude above which we are saving.
	 */
	int ymin,ymax;

	boolean running;

	//UI elements (not described)
	OBJExportOptions options;
	JProgressBar progress;
	JTextField tfSavePath;
	JButton bRun,bStop;
	JButton bOptions;
	JButton bTex;


	/**
	 * Main constructor.
	 * @param objfile path to OBJ file
	 * @param savepath path to world save
	 * @param dimension Dimension to load chunks from
	 * @param bounds region being saved
	 * @param ymin minimum altitude being saved
	 */
	public OBJExportPanel(File savepath, int dimension, Rectangle bounds, int ymin, int ymax) 
	{		
		super("Export selection");		

		this.savepath=savepath;
		this.dimension=dimension;
		this.bounds=bounds;
		this.ymin=ymin;
		this.ymax=ymax;

		setSize(400,200);
		setMinimumSize(new Dimension(400,0));

		JPanel main=new JPanel();
		add(main);

		main.setLayout(new BoxLayout(main, BoxLayout.PAGE_AXIS));

		JPanel pSavePath=new JPanel();
		pSavePath.setMaximumSize(new Dimension(Short.MAX_VALUE,50));
		JLabel lSavePath=new JLabel("Save folder: ");
		pSavePath.setLayout(new BoxLayout(pSavePath, BoxLayout.LINE_AXIS));		
		tfSavePath = new JTextField();		
		JButton bObj=new JButton("Browse");
		pSavePath.add(lSavePath);
		pSavePath.add(tfSavePath);
		pSavePath.add(bObj);
		main.add(pSavePath);

		tfSavePath.setText(MainWindow.settings.getLastExportPath());

		JPanel pOptions=new JPanel();
		pOptions.setLayout(new BoxLayout(pOptions, BoxLayout.LINE_AXIS));
		bOptions=new JButton("Show more options...");
		bOptions.setMaximumSize(new Dimension(Short.MAX_VALUE,50));
		pOptions.add(bOptions);
		main.add(pOptions);

		options=new OBJExportOptions();
		options.setVisible(false);
		main.add(options);

		main.add(Box.createVerticalGlue());

		JPanel pRun=new JPanel();
		pRun.setLayout(new BoxLayout(pRun, BoxLayout.LINE_AXIS));
		bRun=new JButton("Export");
		bRun.setMaximumSize(new Dimension(Short.MAX_VALUE,50));
		Font fRun=bRun.getFont();
		bRun.setFont(new Font(fRun.getFamily(),fRun.getStyle()+Font.BOLD,fRun.getSize()));
		bRun.setForeground(Color.red);
		bStop=new JButton("Stop");
		bStop.setMaximumSize(new Dimension(Short.MAX_VALUE,50));
		bStop.setEnabled(false);
		pRun.add(bRun);
		pRun.add(bStop);
		main.add(pRun);

		JPanel pTex=new JPanel();
		pTex.setLayout(new BoxLayout(pTex, BoxLayout.LINE_AXIS));
		bTex=new JButton("Export Textures");
		bTex.setMaximumSize(new Dimension(Short.MAX_VALUE,50));
		pTex.add(bTex);
		main.add(pTex);

		progress=new JProgressBar();
		progress.setStringPainted(true);
		main.add(progress);

		bObj.addActionListener(new AbstractAction() {			
			@Override
			public void actionPerformed(ActionEvent arg0) {

				JFileChooser jfcSave=new JFileChooser();
				jfcSave.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jfcSave.setDialogTitle("Save folder");
				if(jfcSave.showSaveDialog(OBJExportPanel.this)!=JFileChooser.APPROVE_OPTION)
				{
					bRun.setEnabled(true);
					bStop.setEnabled(false);
					return;
				}

				File save_path=jfcSave.getSelectedFile();													
				tfSavePath.setText(save_path.getAbsolutePath());
			}
		});

		bRun.addActionListener(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {

				bRun.setEnabled(false);
				bStop.setEnabled(true);
				(new Thread(OBJExportPanel.this)).start();

			}
		});

		bStop.addActionListener(new AbstractAction() {			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				running=false;				
			}
		});

		bOptions.addActionListener(new AbstractAction() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!options.isVisible())
				{
					options.setVisible(true);
					bOptions.setText("Hide options...");		
					pack();
				}
				else
				{
					options.setVisible(false);
					bOptions.setText("Show options...");
					pack();
				}
			}
		});

		bTex.addActionListener(new AbstractAction() {			
			@Override
			public void actionPerformed(ActionEvent arg0) {				
				TexsplitDialog tsdiag=new TexsplitDialog(tfSavePath.getText()+"/tex");
				Point p=getLocation();
				p.x+=(getWidth()-tsdiag.getWidth())/2;
				p.y+=(getHeight()-tsdiag.getHeight())/2;
				tsdiag.setLocation(p);
			}
		});

		pack();
		setVisible(true);
	}

	/**
	 * Main thread method.
	 */
	@Override
	public void run() {

		File exportpath=new File(tfSavePath.getText());

		File objfile=new File(exportpath.getAbsolutePath(),MainWindow.file_names.getOBJName());
		File mtlfile=new File(exportpath.getAbsolutePath(),MainWindow.file_names.getMTLName());
		File tmpdir=new File(exportpath.getAbsolutePath()+"/temp");

		boolean write_obj=true;
		boolean write_mtl=true;

		if(tmpdir.exists())
		{
			Log.error("Cannot create directory: "+tmpdir.getAbsolutePath()+"\nSomething is in the way.\nDelete it or turn off the Sort OBJ option.", null);
			bRun.setEnabled(true);
			bStop.setEnabled(false);
			return;
		}

		if(objfile.exists())
		{
			if(options.getOBJOverwriteAction()==OverwriteAction.ALWAYS)
			{
				write_obj=true;
			}
			else if(options.getOBJOverwriteAction()==OverwriteAction.NEVER)
			{
				write_obj=false;
			}
			else if(JOptionPane.showConfirmDialog(OBJExportPanel.this, "OBJ file already exists. Do you want to overwrite?")!=JOptionPane.YES_OPTION)
			{												
				write_obj=false;
			}
		}

		if(mtlfile.exists())
		{
			if(options.getMTLOverwriteAction()==OverwriteAction.ALWAYS)
			{
				write_mtl=true;
			}
			else if(options.getMTLOverwriteAction()==OverwriteAction.NEVER)
			{
				write_mtl=false;
			}
			else if(JOptionPane.showConfirmDialog(OBJExportPanel.this, "MTL file already exists. Do you want to overwrite?")!=JOptionPane.YES_OPTION)
			{												
				write_mtl=false;
			}
		}

		try {
			objfile.createNewFile();
			mtlfile.createNewFile();
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(this, "Cannot write to the chosen location!");
			bRun.setEnabled(true);
			bStop.setEnabled(false);
			return;
		}

		OffsetType offset_type=options.getOffsetType();
		Point offset_point=options.getCustomOffset();
		MainWindow.settings.setLastExportPath(tfSavePath.getText());

		running=true;


		float scale=options.getScale();
		boolean obj_per_mat=options.getObjPerMat();

		options.saveSettings();

		try
		{
			if(write_mtl)
			{
				Materials.copyMTLFile(mtlfile);
				Log.info("Saved materials to "+mtlfile.getAbsolutePath());
			}

			PrintWriter obj_writer=new PrintWriter(new FileWriter(objfile));

			if(write_obj)
			{
				int cxs=(int)Math.floor(bounds.x/16.0f);
				int czs=(int)Math.floor(bounds.y/16.0f);
				int cxe=(int)Math.ceil((bounds.x+bounds.width)/16.0f);
				int cze=(int)Math.ceil((bounds.y+bounds.height)/16.0f);
				int oxs=0,oys=0,ozs=0;

				if(offset_type==OffsetType.NO_OFFSET)
				{
					oxs=0;
					oys=0;
					ozs=0;
				}
				else if(offset_type==OffsetType.CENTER_OFFSET)
				{
					oxs=cxs+(cxe-cxs)/2;
					oys=ymin;
					ozs=czs+(cze-czs)/2;
				}
				else if(offset_type==OffsetType.CUSTOM_OFFSET)
				{
					oxs=offset_point.x;
					oys=0;
					ozs=offset_point.y;
				}


				int progress_count=0;
				int progress_max=(cxe-cxs)*(cze-czs);

				progress.setMaximum(progress_max);

				ChunkDataBuffer chunk_buffer=new ChunkDataBuffer(bounds, ymin, ymax);

				OBJOutputFile obj=new OBJOutputFile("minecraft");
				obj.setOffset(-oxs*16, -oys, -ozs*16);
				obj.setScale(scale);

				obj.appendMtl(obj_writer, mtlfile.getName());
				if(!obj_per_mat) obj.appendObjectname(obj_writer);

				Log.info("Processing chunks...");

				for(int cx=cxs; cx<=cxe && running; cx++)
				{
					for(int cz=czs; cz<=cze && running; cz++,progress_count++)
					{
						progress.setValue(progress_count);					

						for(int lx=cx-1; lx<=cx+1; lx++)
							for(int lz=cz-1; lz<=cz+1; lz++)
							{
								if(lx<cxs || lx>cxe || lz<czs || lz>cze) continue;

								if(chunk_buffer.hasChunk(lx, lz)) continue;

								Chunk chunk=null;
								try{
									Region region=Region.findRegion(savepath, dimension, lx, lz);							
									if(region==null) continue;					
									chunk=region.getChunk(lx, lz);
									if(chunk==null) continue;
								}catch (Exception e) {
									continue;
								}

								chunk_buffer.addChunk(chunk);		
								obj.appendTextures(obj_writer);
								obj.appendNormals(obj_writer);
								obj.appendVertices(obj_writer);
								obj.appendFaces(obj_writer,obj_per_mat);
								obj.clearData(options.getRemoveDuplicates());
							}

						obj.addChunkBuffer(chunk_buffer,cx,cz);

						for(int lx=cx-1; lx<=cx+1; lx++)
							chunk_buffer.removeChunk(lx,cz-1);
					}

					chunk_buffer.removeAllChunks();
				}

				obj_writer.close();

				Log.info("Saved model to "+objfile.getAbsolutePath());
			}


			Log.info("Sorting OBJ file...");

			if(!tmpdir.mkdir())
			{
				Log.error("Cannot temp create directory: "+tmpdir.getAbsolutePath(), null);
				bRun.setEnabled(true);
				bStop.setEnabled(false);
				return;
			}

			File mainfile=new File(tmpdir,"main");
			PrintWriter main=new PrintWriter(mainfile);
			File vertexfile=new File(tmpdir,"vertex");
			PrintWriter vertex=new PrintWriter(vertexfile);
			File normalfile=new File(tmpdir,"normal");
			PrintWriter normal=new PrintWriter(normalfile);
			File uvfile=new File(tmpdir,"uv");
			PrintWriter uv=new PrintWriter(uvfile);

			BufferedReader objin=new BufferedReader(new FileReader(objfile));				

			Map<String,FaceFile> faces=new HashMap<String, FaceFile>();
			int facefilecount=1;

			FaceFile current_ff=null;

			progress.setMaximum(100);
			int maxcount=(int)vertexfile.length();
			if(maxcount==0) maxcount=1;
			int count=0;

			String line;
			while((line=objin.readLine())!=null)
			{
				if(line.length()==0) continue;

				count+=line.length()+1;
				if(count>maxcount) count=maxcount;
				progress.setValue((int)(50.0*(double)count/(double)maxcount));

				if(line.startsWith("usemtl "))
				{
					line=line.substring(7).trim();						

					if(!faces.containsKey(line))
					{
						current_ff=new FaceFile();
						current_ff.name=line;
						current_ff.file=new File(tmpdir,""+facefilecount);
						facefilecount++;
						current_ff.writer=new PrintWriter(current_ff.file);
						faces.put(line, current_ff);
					}
					else
						current_ff=faces.get(line);
				}
				else if(line.startsWith("f "))
				{
					if(current_ff!=null)
					{
						current_ff.writer.println(line);
					}
				}
				else if(line.startsWith("v "))
				{
					vertex.println(line);
				}	
				else if(line.startsWith("vn "))
				{
					normal.println(line);
				}	
				else if(line.startsWith("vt "))
				{
					uv.println(line);
				}	
				else if(options.getObjPerMat() && line.startsWith("g "))
				{
					continue;
				}
				else
				{						
					main.println(line);
					if(line.startsWith("mtllib") || line.startsWith("g "))
						main.println();
				}
			}

			objin.close();

			vertex.close();
			normal.close();
			uv.close();

			BufferedReader norm_reader=new BufferedReader(new FileReader(normalfile));
			while((line=norm_reader.readLine())!=null)
				main.println(line);
			norm_reader.close();
			normalfile.delete();

			BufferedReader uv_reader=new BufferedReader(new FileReader(uvfile));
			while((line=uv_reader.readLine())!=null)
				main.println(line);
			uv_reader.close();
			uvfile.delete();

			BufferedReader vertex_reader=new BufferedReader(new FileReader(vertexfile));
			while((line=vertex_reader.readLine())!=null)
				main.println(line);
			vertex_reader.close();
			vertexfile.delete();

			count=0;
			maxcount=faces.size();

			for(FaceFile ff:faces.values())
			{
				ff.writer.close();

				count++;
				progress.setValue(50+(int)(50.0*(double)count/(double)maxcount));

				vertex.println();
				if(options.getObjPerMat()) main.println("g "+ff.name);
				main.println("usemtl "+ff.name);
				main.println();

				BufferedReader reader=new BufferedReader(new FileReader(ff.file));
				while((line=reader.readLine())!=null)
				{
					main.println(line);
				}
				reader.close();

				ff.file.delete();
			}

			main.close();

			//Files.move(mainfile.toPath(), objfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Filesystem.moveFile(mainfile, objfile);

			if(!tmpdir.delete())
			{
				Log.error("Failed to erase temp dir: "+tmpdir.getAbsolutePath()+"\nPlease remove it yourself!", null);
			}


			Log.info("Done!");

		}
		catch (Exception e) {
			Log.error("Error while exporting OBJ:", e);
		}

		bRun.setEnabled(true);
		bStop.setEnabled(false);
	}
}

/**
 * Little helper class for the map used in sorting.
 * @author danijel
 *
 */
class FaceFile
{
	public String name;
	public File file;
	public PrintWriter writer;
};