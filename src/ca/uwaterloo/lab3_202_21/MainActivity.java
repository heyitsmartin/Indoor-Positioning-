package ca.uwaterloo.lab3_202_21;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import mapper.*;



public class MainActivity extends Activity {
	TextView output;
	TextView output2;
	Button button;
	Button sbutton;
	int c;
	float r =0.75f;
	float average = 0;
	boolean step = false;
	float azimuth;
	float north;
	float east;
	float displacement;
    public float[] mfvalues;
	public float[] avalues;
	public float[] rotationMatrix = new float[9];
	public float[] orientation = new float[3];
	TextView tv ;
	public MapView mv;
	public PositionListener pos;
	boolean reset = false; 
	float dn; 
	float de;
	
	//public int stepLength = c;
	
	//Big Group of Stuff
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);  
		LinearLayout layout= (LinearLayout)findViewById(R.id.label1);
        layout.setOrientation(LinearLayout.VERTICAL);
        
        TextView tv1 = new TextView(getApplicationContext());
        layout.addView(tv1);
         tv = (TextView) findViewById(R.id.textview6); 
        tv.setText(" ");
        
        
		//SensorManager
		SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		SensorEventListener m = new getSensor(tv1);
		sensorManager.registerListener(m, accelerometer,SensorManager.SENSOR_DELAY_UI);
		
		Sensor accelerometerL = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		SensorEventListener a = new getSensor(null);
		sensorManager.registerListener(a, accelerometerL,SensorManager.SENSOR_DELAY_UI);
		
        Sensor magneticfield = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        SensorEventListener n = new getSensor(tv1);
        sensorManager.registerListener(n, magneticfield,SensorManager.SENSOR_DELAY_UI);


        
        
       
        
        
        //Reset Button
        button = (Button) findViewById(R.id.button1); 

		button.setOnClickListener( new OnClickListener() { 
			@Override
			public void onClick(View arg0) {
				c=0;
				north = 0;
				east = 0;
				displacement = 0;
				
				
				
			}
		});
		//Map Stuff
			mv = new MapView(getApplicationContext(), 1080,1980,40,40);
			registerForContextMenu ( mv );
			System.out.println("bob "+getExternalFilesDir ( null ));
			NavigationalMap map = MapLoader.loadMap ( getExternalFilesDir ( null ) ,
					"d.svg");
			pos = new PositionListen (map);
			mv.setMap( map );
			layout.addView(mv);
			mv.addListener(pos);		
		
		//Step Button
		sbutton = (Button) findViewById(R.id.button2);
		sbutton.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View arg0){
				c++;
				north += (float) Math.cos(azimuth);
				east  += (float) Math.sin(azimuth);
				
				displacement = (float)Math.sqrt(Math.pow(north, 2)+ Math.pow(east, 2));
				PointF currentP = mv.getUserPoint();
				//mv.setUserPoint( currentP.x + (float) Math.sin(azimuth), currentP.y - (float) Math.cos(azimuth));
				((PositionListen) pos).updateUser(); 
				
				
				
				
			}
		});
				
	   
		
	}
	
	//Random Stuff
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onCreateContextMenu ( ContextMenu menu , View v , ContextMenuInfo menuInfo ) {
		super.onCreateContextMenu ( menu , v , menuInfo );
		mv.onCreateContextMenu ( menu , v , menuInfo );
	}
	
	@Override
	public boolean onContextItemSelected ( MenuItem item ) {
		return super.onContextItemSelected ( item ) || mv.onContextItemSelected ( item );
	}
	
	//Non-Random Stuff
	class getSensor implements SensorEventListener 
	{
		public float x;
		public float y;
		public float z;
	
		public getSensor(TextView outputView)
		{
			output = outputView;
		}

		public void onAccuracyChanged(Sensor s, int i) {}
		
		public void onSensorChanged(SensorEvent se) 
		{ 

			switch(se.sensor.getType()){
			
			case Sensor.TYPE_LINEAR_ACCELERATION:
				x = se.values[0];
				y = se.values[1];
				z = se.values[2];
			
				average = (-y); 
				
				if (average>r && !step){ 
					c++;
					step = true; 
					north += (float) Math.cos(azimuth);
					east  += (float) Math.sin(azimuth);
					displacement = (float)Math.sqrt(Math.pow(north, 2)+ Math.pow(east, 2));
					PointF currentP = mv.getUserPoint();
					
					
				}
				
				if (average <r && step) { 
					step = false; 
				}
				
				break;
				
			case Sensor.TYPE_ACCELEROMETER: 
				
				avalues = se.values;
				break;
				
			case Sensor.TYPE_MAGNETIC_FIELD:
	    		mfvalues = se.values;
	    		break;
			} 
			
			if(avalues != null && mfvalues != null) {
				boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, avalues, mfvalues);
		
		        if(success)
		        {
		        	SensorManager.getOrientation(rotationMatrix, orientation);
		        	
		        }
			}
			
			azimuth = (float)(orientation[0]);
						
			output.setText("NORTH:" + north + "\nEAST:" + east + "\nDIS:"+displacement+"\nSteps:" + c +"\nCurrent degrees from N:"+(double)Math.round(Math.toDegrees(orientation[0]) * 1000) / 1000) ;
		}
	} 
	
	//New Stuff
	class PositionListen implements PositionListener{

		NavigationalMap nmap;
		List<InterceptPoint> hits = new ArrayList<InterceptPoint>();
		List<PointF> points = new ArrayList<PointF>();
		public void updateUser() { 
			PointF newUserPoint = new PointF(mv.getUserPoint().x+ (float) Math.sin(azimuth), mv.getUserPoint().y- (float) Math.cos(azimuth)); 
			if(nmap.calculateIntersections(mv.getUserPoint(), newUserPoint).isEmpty())
			{
				mv.setUserPoint( mv.getUserPoint().x + (float) Math.sin(azimuth), mv.getUserPoint().y - (float) Math.cos(azimuth));
			}

			// Check is user arrived at destination 
			//allow for error ~ 
			
			if( Math.abs(mv.getUserPoint().x - mv.getDestinationPoint().x) <= .8){ 
				if(Math.abs(mv.getUserPoint().y - mv.getDestinationPoint().y) <= .8) { 
					tv.setText("YOU HAVE REACHED THE END");
				}
			}else { 
				float dx = mv.getUserPoint().x - mv.getDestinationPoint().x; 
				float dy = Math.abs(mv.getUserPoint().y - mv.getDestinationPoint().y);
				String  ew ; 
				String ns; 
				if (dx > 0) { 
					ew = "West"; 
				}else{ 
					ew = "East";
				}
				
				if( dy < 0 ) { 
					ns = "South";
				}else { 
					ns = "North"; 
				}
				dy = Math.abs(dy); 
				dx = Math.abs(dx); 
				tv.setText("You need to move " +dx +" steps towards "+ ew +"\nYou need to move "+ dy +" steps towards " + ns);
				
			}
		
		}
		public PositionListen(NavigationalMap map){
			nmap = map;
		}
		public void reset() { 
			points = null ; 
			hits = null; 
			hits = new ArrayList<InterceptPoint>();
			points = new ArrayList<PointF>();
		}
		
		@Override
		public void originChanged(MapView source, PointF loc) {
			mv.setUserPoint(loc.x, loc.y);
		
			pathFinder();
		}

		@Override
		public void destinationChanged(MapView mv, PointF dest) {
			
			pathFinder();
		}	
		
		public void pathFinder(){	
			mv.invalidate();
			
			
		
			
			hits = nmap.calculateIntersections(mv.getOriginPoint(), mv.getDestinationPoint());
			System.out.println("bob "+ hits.isEmpty());
			
			if(!hits.isEmpty())
			{
				PointF tempO = new  PointF(mv.getOriginPoint().x,mv.getOriginPoint().y);
				PointF tempN = new  PointF(mv.getOriginPoint().x,mv.getOriginPoint().y);
				PointF tempD = new  PointF(mv.getDestinationPoint().x,mv.getDestinationPoint().y);
				PointF tempK = new  PointF(mv.getDestinationPoint().x,mv.getDestinationPoint().y);
				float nav = .01f; 
				float nav2 = nav;
				for(int i = 0 ; i< 300 ; i ++ ) { 
					
					tempO.y += nav;
					tempD.y = tempO.y; 
					tempN.y = tempO.y + 5*nav;
					
					tempO.x = mv.getOriginPoint().x ; 
					tempD.x = mv.getDestinationPoint().x;
					
					
					
					if(nmap.calculateIntersections(tempO, tempD).isEmpty()){
						points.add(mv.getOriginPoint());
						points.add(tempO);
						points.add(tempD); 
						points.add(mv.getDestinationPoint());
						mv.setUserPath(points);
						
						tempO = null ; 
						tempD = null; 
						tempN = null;
						points.clear();
						break; 
					}
					
					if(!nmap.calculateIntersections(tempO, tempN).isEmpty()){
						System.out.println("BOB FLIPPED DIRECTIONS");
						nav *= -1;
					}
					
				}
				
			}else if ( hits.isEmpty())
			{	
				points.add(mv.getOriginPoint());
				System.out.println("bob YO ITS EMPTY DOE ");
				points.add(mv.getDestinationPoint());
				mv.setUserPath(points);
			}
			hits=null;
		}
	}
}