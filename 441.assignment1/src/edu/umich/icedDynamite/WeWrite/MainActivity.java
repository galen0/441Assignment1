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
  private CustomEditText broadcastText;
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
  //private boolean recvToggle = false;
  private TextAction broadcastData;
  
  // Undo and Redo action stacks
  Stack<TextAction> undoStack = new Stack<TextAction>();
  Stack<TextAction> redoStack = new Stack<TextAction>();
  
  //Toggle Options button text
  public void toggleOptions(View v) {
      showToast("Checking " + handleButton.getText().toString());
	  if(handleButton.getText().toString().equals("[+] Show Options"))
		  handleButton.setText("[-] Hide Options");
	  else
		  handleButton.setText("[:|] Show Options");
  }
  
  // Apply an action
  public void applyAction(TextAction recvText){
	  //int prevLocation = broadcastText.getSelectionEnd();
	  Editable text = broadcastText.getText();
	  
      broadcastText.removeTextChangedListener(broadcastTextWatcher);
      
      Log.d("RECEIVE", recvText.text);
      Log.d("RECEIVE", Integer.toString(recvText.location));
      
      if(recvText.backspace == false) {
    	  text.insert(recvText.location, recvText.text);
    	  broadcastText.setText(text);
    	  //broadcastText.getText().replace(recvText.location, recvText.location+1, recvText.text);
      }
      else {
	      broadcastText.setSelection(recvText.location);
    	  broadcastText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
      }
      broadcastText.setSelection(broadcastText.getSelectionEnd());
      
      broadcastText.addTextChangedListener(broadcastTextWatcher);
  }
  
  // Revert an action
  public void revertAction(TextAction action){
	  //TODO: Implement applyAction
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
    	  
//	      Utils.printMethodName(TAG);
//	      String message = new String(data);
//	      broadcastText.removeTextChangedListener(broadcastTextWatcher);
//	      broadcastText.setText(message);
//	      broadcastText.setSelection(broadcastText.getText().length());
//	      broadcastText.addTextChangedListener(broadcastTextWatcher);
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
    Log.d("parid", Long.toString(participantId));
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
        broadcastText.setText("");
        displayNameButton.setEnabled(false);
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
      }
    });
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    broadcastText = (CustomEditText) findViewById(R.id.broadcastText);
    connectButton = (Button) findViewById(R.id.ConnectButton);

    joinButton = (Button) findViewById(R.id.getSessionButton);
    leaveButton = (Button) findViewById(R.id.LeaveSessionButton);
    leaveButton.setEnabled(false);
    undoButton = (Button) findViewById(R.id.RedoButton);
    undoButton.setEnabled(false);
    redoButton = (Button) findViewById(R.id.UndoButton);
    redoButton.setEnabled(false);
    displayNameButton = (Button) findViewById(R.id.DisplayNameButton);
    displayNameButton.setText("Change Display Name (" + DISPLAY_NAME + ")");
    handleButton = (Button) findViewById(R.id.handle);
   
    broadcastTextWatcher = new TextWatcher(){
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			broadcastData = new TextAction();
			broadcastData.senderId = participantId;
			if (count < before){
				Log.d("KEY_EVENT", "typed a backspace at " + Integer.toString(start+before));
				broadcastData.text = "0";
				broadcastData.backspace = true;
				broadcastData.location = broadcastText.getSelectionEnd();
			}
			else if (count > before){
				broadcastData.backspace = false;
				if (start == 0 && before == 0 && count == 1){ //beginning
					Log.d("KEY_EVENT", "typed: " + s.toString().substring(0,1) + " at beginning");	
					broadcastData.text = s.toString().substring(0,1);
					broadcastData.location = broadcastText.getSelectionEnd();
				}
				else if (start != 0 && count == 1){ //middle
					Log.d("KEY_EVENT", "typed: " + s.toString().substring(start, start+count) + " at " + Integer.toString(start));
					broadcastData.text = s.toString().substring(start, start+count);
					broadcastData.location = broadcastText.getSelectionEnd();
				}
				else{
					Log.d("KEY_EVENT", "typed: " + s.toString().substring(before, count) + " at " + Integer.toString(before+start));
					broadcastData.text = s.toString().substring(before, count);
					broadcastData.location = broadcastText.getSelectionEnd();
				}
			}
			else {
				Log.d("KEY_EVENT", "else: " + s.toString());
				broadcastData.broadcast = false;
			}
			
			//Log.d("KEY_EVENT", "start: " + start + " before: " + before + " count: " + count);
			Log.d("KEY_EVENT", "++++++++++++++++++++");
			if(broadcastData.broadcast) {
				Log.d("BROADCAST", "BROADCAST");
				doBroadcast(getWindow().getDecorView().findViewById(android.R.id.content), broadcastData);

				// Add the action to the undo stack
				undoStack.push(broadcastData);
				undoButton.setEnabled(true);
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
		  applyAction(action);
		  
		  //place action on redo stack
		  redoStack.push(action);
		  
		  //Disable undo button if stack is empty
		  if(undoStack.empty())
			  undoButton.setEnabled(false);
		  
		  //Enable redoButton
		  redoButton.setEnabled(true);
	  }
		  
  }
  
  //Redoes the last action undone by the user
  public void redo(View v)
  {
	  if(!redoStack.empty()) {
		  
		  //Pop action off redo stack & apply it
		  TextAction action = undoStack.pop();
		  revertAction(action);
		  
		  //place action on undo stack
		  undoStack.push(action);
		  
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
    	// Create and serialize textAction with location and text
    	//TextAction broadcastInfo = new TextAction();
    	//broadcastInfo.location = broadcastText.getSelectionEnd();
    	
    	myClient.broadcast(serialize(broadcastData), "lol", broadcastListener);

//        myClient.broadcast(broadcastText.getText().toString().getBytes(),
//            "lol", broadcastListener);
        
        showToast(broadcastData.text + " broadcasted");
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
