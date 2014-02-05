package edu.umich.icedDynamite.WeWrite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import edu.umich.icedDynamite.WeWrite.R;
import edu.umich.imlc.android.common.Utils;
import edu.umich.imlc.collabrify.client.CollabrifyClient;
import edu.umich.imlc.collabrify.client.CollabrifyListener.CollabrifyBroadcastListener;
import edu.umich.imlc.collabrify.client.CollabrifyListener.CollabrifyCreateSessionListener;
import edu.umich.imlc.collabrify.client.CollabrifyListener.CollabrifyJoinSessionListener;
import edu.umich.imlc.collabrify.client.CollabrifyListener.CollabrifyLeaveSessionListener;
import edu.umich.imlc.collabrify.client.CollabrifyListener.CollabrifyListSessionsListener;
import edu.umich.imlc.collabrify.client.CollabrifyListener.CollabrifySessionListener;
import edu.umich.imlc.collabrify.client.CollabrifyParticipant;
import edu.umich.imlc.collabrify.client.CollabrifySession;
import edu.umich.imlc.collabrify.client.exceptions.CollabrifyException;
import edu.umich.imlc.collabrify.client.exceptions.CollabrifyUnrecoverableException;

public class MainActivity extends Activity implements
    CollabrifySessionListener, CollabrifyListSessionsListener,
    CollabrifyBroadcastListener, CollabrifyCreateSessionListener,
    CollabrifyJoinSessionListener, CollabrifyLeaveSessionListener
{
  private static String TAG = "dummy";

  private static final String GMAIL = "iceddynamite@umich.edu";
  private static String DISPLAY_NAME = "New User";
  private static final String ACCOUNT_GMAIL = "441winter2014@umich.edu";
  private static final String ACCESS_TOKEN = "338692774BBE";

  private CollabrifyClient myClient;
  private EditText broadcastText;
  private Button connectButton;
  private Button joinButton;
  private Button leaveButton;
  private Button undoButton;
  private Button redoButton;
  private Button displayNameButton;
  private Button handleButton;
  private ArrayList<String> tags = new ArrayList<String>();
  private long sessionId;
  private String sessionName;
  private long participantId;
  private String password = "password";
  
  // redundant but for the sake of readability
  private CollabrifySessionListener sessionListener = this;
  private CollabrifyListSessionsListener listSessionsListener = this;
  private CollabrifyBroadcastListener broadcastListener = this;
  private CollabrifyCreateSessionListener createSessionListener = this;
  private CollabrifyJoinSessionListener joinSessionListener = this;
  private CollabrifyLeaveSessionListener leaveSessionListener = this;

  private TextWatcher broadcastTextWatcher;
  private boolean undoToggle = false;
  private TextAction broadcastData;
  
  // Undo and Redo action stacks
  Stack<TextAction> undoStack = new Stack<TextAction>();
  Stack<TextAction> redoStack = new Stack<TextAction>();
  
  public void shiftRight(TextAction action) {
	  TextAction temp;
	  for(int i = 0; i < undoStack.size(); i++) {
		  temp = undoStack.elementAt(i);
		  if(temp.location > action.location) {
			  temp.location++;
			  undoStack.set(i, temp);
		  }
	  }
	  for(int i = 0; i < redoStack.size(); i++) {
		  temp = redoStack.elementAt(i);
		  if(temp.location > action.location) {
			  temp.location++;
			  redoStack.set(i, temp);
		  }
	  }
  }
  
  public void shiftLeft(TextAction action) {
	  TextAction temp;
	  for(int i = 0; i < undoStack.size(); i++) {
		  temp = undoStack.elementAt(i);
		  if(temp.location > (action.location-1)) {
			  temp.location--;
			  undoStack.set(i, temp);
		  }
	  }
	  for(int i = 0; i < redoStack.size(); i++) {
		  temp = redoStack.elementAt(i);
		  if(temp.location > (action.location-1)) {
			  temp.location--;
			  redoStack.set(i, temp);
		  }
	  }
  }
  
  // Apply an action
  public void applyAction(TextAction recvText){
	  Editable text = broadcastText.getText();
	  
      broadcastText.removeTextChangedListener(broadcastTextWatcher);
      
      Log.d("RECEIVE", recvText.text);
      Log.d("RECEIVE", Integer.toString(recvText.location));
      
      if(recvText.backspace == false) {
    	  text.insert(recvText.location, recvText.text);
    	  shiftRight(recvText);
    	  broadcastText.setText(text);
      }
      else {
    	  shiftLeft(recvText);
	      broadcastText.setSelection(recvText.location);
    	  broadcastText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
      }
      broadcastText.setSelection(broadcastText.getSelectionEnd());
      
      broadcastText.addTextChangedListener(broadcastTextWatcher);
  }
 
  // Convert object to byte array
  public static byte[] serialize(Object obj) throws IOException {
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      ObjectOutputStream o = new ObjectOutputStream(b);
      o.writeObject(obj);
      return b.toByteArray();
  }
  
  // Convert byte array to object
  public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
      ByteArrayInputStream b = new ByteArrayInputStream(bytes);
      ObjectInputStream o = new ObjectInputStream(b);
      return o.readObject();
  }
  
  @Override
  public void onReceiveEvent(long orderId, int submissionRegistrationId,
      String eventType, final byte[] data, long elapsed)
  {
    Utils.printMethodName(TAG);
    runOnUiThread(new Runnable()
    {

      @Override
      public void run()
      {
    	try {
    	  TextAction recvText = (TextAction) deserialize(data);
    	  if (recvText.senderId != participantId){
    		  Log.d("sid", Long.toString(recvText.senderId));
    		  applyAction(recvText);
    	  }
		} 
        catch (Exception e) {
		  // TODO Auto-generated catch block
		  e.printStackTrace();
		}
      }
    });
  }

  @Override
  public void onError(CollabrifyException e)
  {
    if(e instanceof CollabrifyUnrecoverableException)
    {
      //the client has been reset and we are no longer in a session
      onDisconnect();
    }
    Log.e(TAG, "error", e);
  }

  @Override
  public void onBroadcastDone(final byte[] event, long orderId, long srid)
  {
    //showToast(new String(event) + " broadcasted");
  }

  @Override
  public void onSessionCreated(final CollabrifySession session)
  {
    sessionId = session.id();
    sessionName = session.name().substring(14, session.name().length()-1);
    participantId = myClient.currentSessionParticipantId();
    Log.d("id", Long.toString(participantId));
    runOnUiThread(new Runnable()
    {

      @Override
      public void run()
      {
        showToast("Session created: " + sessionName);
        connectButton.setText(sessionName);
        connectButton.setEnabled(false);
        joinButton.setEnabled(false);
        leaveButton.setEnabled(true);
        displayNameButton.setEnabled(false);
        broadcastText.setText("");
        broadcastText.setEnabled(true);
      }
    });
  }

  @Override
  public void onSessionJoined(long maxOrderId, long baseFileSize)
  {
    sessionName = myClient.currentSession().name().substring(14, myClient.currentSession().name().length()-1);
    sessionId = myClient.currentSession().id();
    participantId = myClient.currentSessionParticipantId();
    runOnUiThread(new Runnable()
    {

      @Override
      public void run()
      {
        showToast("Session Joined: " + sessionName);
        connectButton.setText(sessionName);
        connectButton.setEnabled(false);
        joinButton.setEnabled(false);
        leaveButton.setEnabled(true);
        displayNameButton.setEnabled(false);
        broadcastText.setText("");
        broadcastText.setEnabled(true);
      }
    });
  }

  @Override
  public void onReceiveSessionList(List<CollabrifySession> sessionList)
  {
	    //Filter out sessions
	  	List<CollabrifySession> newSessionList = new ArrayList<CollabrifySession>();
	    for( CollabrifySession s : sessionList )
	    {
	      	//Filter by Iced Dynamite
	    	if(s.name().startsWith("Iced Dynamite"))
	    		newSessionList.add(s);
	    }
    if( newSessionList.isEmpty() )
    {
      showToast("No sessions available");
      return;
    }
    displaySessionList(newSessionList);
  }

  @Override
  public void onDisconnect()
  {
    runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        showToast("Left session: " + sessionName);
        connectButton.setText("Create Session");
        connectButton.setEnabled(true);
        joinButton.setEnabled(true);
        leaveButton.setEnabled(false);
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        displayNameButton.setEnabled(true);
        broadcastText.setText("");
        broadcastText.setEnabled(false);
      }
    });
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    broadcastText = (EditText) findViewById(R.id.broadcastText);
    
    connectButton = (Button) findViewById(R.id.ConnectButton);

    joinButton = (Button) findViewById(R.id.getSessionButton);
    leaveButton = (Button) findViewById(R.id.LeaveSessionButton);
    leaveButton.setEnabled(false);
    undoButton = (Button) findViewById(R.id.UndoButton);
    undoButton.setEnabled(false);
    redoButton = (Button) findViewById(R.id.RedoButton);
    redoButton.setEnabled(false);
    displayNameButton = (Button) findViewById(R.id.DisplayNameButton);
    displayNameButton.setText("Change Display Name (" + DISPLAY_NAME + ")");
    handleButton = (Button) findViewById(R.id.handle);
    broadcastText.setText("");
    broadcastText.setEnabled(false);
   
    broadcastTextWatcher = new TextWatcher(){
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			broadcastData = new TextAction();
			broadcastData.senderId = participantId;
			int cursorEnd = broadcastText.getSelectionEnd();
			if (count < before){ //backspace
				Log.d("KEY_EVENT", "typed a backspace at " + (cursorEnd+1));
				broadcastData.text = "0";
				broadcastData.backspace = true;
				broadcastData.location = cursorEnd + 1;
				//Log.d("test2", Integer.toString(broadcastText.getSelectionEnd()+1) + " " + Integer.toString(start + before));
			}
			else if (count > before){
				broadcastData.backspace = false;
				if (start == 0 && before == 0 && count == 1){ //beginning
					Log.d("KEY_EVENT", 
							"typed: " + 
							s.toString().substring(cursorEnd - 1, cursorEnd) + 
							" at " + 
							(cursorEnd-1)
					);
					
					broadcastData.text = s.toString().substring(cursorEnd - 1, cursorEnd);
					broadcastData.location = cursorEnd - 1;
				}
				else if (start != 0 && count == 1){ //middle
					Log.d("KEY_EVENT", "typed: " + 
							s.toString().substring(cursorEnd - 1, cursorEnd) + 
							" at " + 
							(cursorEnd-1)
					);
					
					broadcastData.text = s.toString().substring(cursorEnd - 1, cursorEnd);
					broadcastData.location = cursorEnd - 1;

				}
				else{
					Log.d("KEY_EVENT", 
							"typed: " + 
							s.toString().substring(cursorEnd - 1, cursorEnd) + 
							" at " + 
							(cursorEnd-1)
					);
					
					broadcastData.text = s.toString().substring(cursorEnd - 1, cursorEnd);
					broadcastData.location = cursorEnd - 1;
				}
			}
			else {
				broadcastData.broadcast = false;
			}
			
			if(broadcastData.broadcast) {
				Log.d("BROADCAST", "BROADCAST");
				doBroadcast(getWindow().getDecorView().findViewById(android.R.id.content), broadcastData);

				if(!undoStack.empty())
					undoButton.setEnabled(true);
				else
					undoButton.setEnabled(false);
				if(!redoStack.empty())
					redoButton.setEnabled(true);
				else
					redoButton.setEnabled(false);
				
				if(undoToggle == false) {
					// Add the action to the undo stack
					Log.d("UNDO", "PUSH UNDO STACK");
					undoStack.push(broadcastData);
					undoButton.setEnabled(true);
				}
				else
					undoToggle = false;
			}
		}

		@Override
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
		}
    	
    };
    broadcastText.addTextChangedListener(broadcastTextWatcher);

    // Instantiate client object
    try
    {
      myClient = CollabrifyClient.newClient(this, GMAIL, DISPLAY_NAME,
          ACCOUNT_GMAIL, ACCESS_TOKEN, false);
    }
    catch( InterruptedException e )
    {
      Log.e(TAG, "error", e);
    }
    catch( ExecutionException e )
    {
      Log.e(TAG, "error", e);
    }


    tags.add("sample");
  }
  
  public void changeName(View v) {
	  	// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setText(DISPLAY_NAME);
		  new AlertDialog.Builder(this)
		    .setTitle("Change Display Name")
		    .setMessage("Enter the new user display name.")
		    .setView(input)
		    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		        	//change the display name
		        	DISPLAY_NAME = input.getText().toString();
		        	displayNameButton.setText("Change Display Name (" + DISPLAY_NAME + ")");
		        	
		        	// Instantiate client object
		            try
		            {
		              myClient = CollabrifyClient.newClient(MainActivity.this, GMAIL, DISPLAY_NAME,
		                  ACCOUNT_GMAIL, ACCESS_TOKEN, false);
		            }
		            catch( InterruptedException e )
		            {
		              Log.e(TAG, "error", e);
		            }
		            catch( ExecutionException e )
		            {
		              Log.e(TAG, "error", e);
		            }


		            tags.add("sample");
		        }
		    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		            // Do nothing.
		        }
		    }).show();
	  
  }
  
  //Undoes the last action done by the user
  public void undo(View v)
  {
	  if(!undoStack.empty()) {
		  
		  //Pop action off undo stack & revert it
		  TextAction action = undoStack.pop();
		  Log.d("UNDO", Integer.toString(action.location));
	      broadcastText.setSelection(action.location+1);
	      undoToggle = true;
		  broadcastText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
	      broadcastText.setSelection(broadcastText.getSelectionEnd());
		  
		  //place action on redo stack
		  redoStack.push(action);
		  
		  //Disable undo button if stack is empty
		  if(undoStack.empty())
			  undoButton.setEnabled(false);
		  
		  //Enable redoButton
		  redoButton.setEnabled(true);
	  }
		  
  }
  
  public void redo(View v)
  {
	  if(!redoStack.empty()) {
		  
		  //Pop action off redo stack & apply it
		  TextAction action = redoStack.pop();
		  Log.d("REDO", Integer.toString(action.location));
		  Editable text = broadcastText.getText();

	      if(action.backspace == false) {
	    	  text.insert(action.location, action.text);
	    	  broadcastText.setText(text);
		      broadcastText.setSelection(action.location+1);
	      }
	      else {
		      broadcastText.setSelection(action.location);
	    	  broadcastText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
		      broadcastText.setSelection(action.location-1);
	      }
		  
		  //place action on undo stack
		  //undoStack.push(action);
		  
		  //Disable redo button if stack is empty
		  if(redoStack.empty())
			  redoButton.setEnabled(false);
		  
		  //Enable undoButton
		  undoButton.setEnabled(true);
	  }
  }
  
  public void doBroadcast(View v, TextAction broadcastData)
  {
    if( broadcastText.getText().toString().isEmpty() )
      return;
    if( myClient != null && myClient.inSession() )
    {
      try
      {
    	
    	myClient.broadcast(serialize(broadcastData), "lol", broadcastListener);

        //showToast(broadcastData.text + " broadcasted");
      }
      catch( CollabrifyException e )
      {
        onError(e);
      }
      catch( Exception e )
      {
		  e.printStackTrace();
      }
    }
  }

  public void doCreateSession(View v)
  {
	// Set an EditText view to get user input 
	final EditText input = new EditText(this);
	
	  new AlertDialog.Builder(this)
	    .setTitle("New Session")
	    .setMessage("Enter the new session name.")
	    .setView(input)
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	String name = input.getText().toString();
				sessionName = "Iced Dynamite(" + name + ")";
				try{
					myClient.createSession(sessionName, tags, password, 0,
							createSessionListener, sessionListener);
				}
			    catch( CollabrifyException e )
			    {
			      onError(e);
			    } 
	        }
	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            // Do nothing.
	        }
	    }).show();
  }

  public void doJoinSession(View v)
  {
    if( myClient.inSession() )
    {
      return;
    }
    try
    {
      myClient.requestSessionList(tags, listSessionsListener);
    }
    catch( Exception e )
    {
      Log.e(TAG, "error", e);
    }
  }

  public void doLeaveSession(View v)
  {
    if( !myClient.inSession() )
    {
      return;
    }
    try
    {
      myClient.leaveSession(false, leaveSessionListener);
    }
    catch( CollabrifyException e )
    {
      onError(e);
    }
  }


  /*
   * (non-Javadoc)
   * 
   * @see android.app.Activity#onDestroy()
   */
  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    if( myClient != null && myClient.inSession() )
    {
      try
      {
        myClient.leaveSession(true, null);
      }
      catch( CollabrifyException e )
      {
        onError(e);
      }
    }
  }

  private void displaySessionList(final List<CollabrifySession> sessionList)
  {
    // Create a list of session names
    List<String> sessionNames = new ArrayList<String>();
    for( CollabrifySession s : sessionList ) {
    	//Strip the surrounding "Iced Dynamite(_)" from the name
    	String str = s.name().substring(14, s.name().length()-1);
    	sessionNames.add(str);
    }
    
    // create a dialog to show the list of session names to the user
    final AlertDialog.Builder builder = new AlertDialog.Builder(
        MainActivity.this);
    builder.setTitle("Choose Session");
    builder.setItems(sessionNames.toArray(new String[sessionList.size()]),
        new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            // when the user chooses a session, join it
            try
            {
              sessionId = sessionList.get(which).id();
              myClient.joinSession(sessionId, password, joinSessionListener,
                  sessionListener);
            }
            catch( CollabrifyException e )
            {
              onError(e);
            }
          }
        });

    // display the dialog
    runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        builder.show();
      }
    });
  }

  private void showToast(final String text)
  {
    runOnUiThread(new Runnable()
    {

      @Override
      public void run()
      {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onBaseFileUploadComplete(long baseFileSize)
  {
    // unused, this example does not use any base files
  }

  @Override
  public void onBaseFileReceived(File baseFile)
  {
    // unused, this example does not use any base files
  }

  @Override
  public void onParticipantJoined(CollabrifyParticipant p)
  {
    showToast(p.getDisplayName() + " has joined");
  }

  @Override
  public void onParticipantLeft(CollabrifyParticipant p)
  {
    showToast(p.getDisplayName() + " has left");
  }

  @Override
  public void onSessionEnd(long id)
  {
    // unused since we don't provide an interface to end the session
  }

  @Override
  public void onFurtherJoinsPrevented()
  {
    // unused since we don't provide an interface to prevent further joins
  }

}
