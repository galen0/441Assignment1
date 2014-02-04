package edu.umich.icedDynamite.WeWrite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import edu.umich.icedDynamite.WeWrite.TextAction;

public class MainActivity extends Activity implements
    CollabrifySessionListener, CollabrifyListSessionsListener,
    CollabrifyBroadcastListener, CollabrifyCreateSessionListener,
    CollabrifyJoinSessionListener, CollabrifyLeaveSessionListener
{
  private static String TAG = "dummy";

  private static final String GMAIL = "iceddynamite@umich.edu";
  private static final String DISPLAY_NAME = "A user";
  private static final String ACCOUNT_GMAIL = "441winter2014@umich.edu";
  private static final String ACCESS_TOKEN = "338692774BBE";

  private CollabrifyClient myClient;
  private EditText broadcastText;
  private Button connectButton;
  private ArrayList<String> tags = new ArrayList<String>();
  private long sessionId;
  private String sessionName;
  private String password = "password";
  
  
  // redundant but for the sake of readability
  private CollabrifySessionListener sessionListener = this;
  private CollabrifyListSessionsListener listSessionsListener = this;
  private CollabrifyBroadcastListener broadcastListener = this;
  private CollabrifyCreateSessionListener createSessionListener = this;
  private CollabrifyJoinSessionListener joinSessionListener = this;
  private CollabrifyLeaveSessionListener leaveSessionListener = this;

  // Undo and Redo action stacks
  Stack<TextAction> undoStack = new Stack<TextAction>();
  Stack<TextAction> redoStack = new Stack<TextAction>();
  
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
    	  broadcastText.getText().replace(recvText.location, 1, recvText.text);
    	  
	      Utils.printMethodName(TAG);
	      String message = new String(data);
	      broadcastText.setText(message);
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
    showToast(new String(event) + " broadcasted");
  }

  @Override
  public void onSessionCreated(final CollabrifySession session)
  {
    sessionId = session.id();
    sessionName = session.name();
    runOnUiThread(new Runnable()
    {

      @Override
      public void run()
      {
        showToast("Session created, id: " + session.id());
        connectButton.setText(sessionName);
      }
    });
  }

  @Override
  public void onSessionJoined(long maxOrderId, long baseFileSize)
  {
    sessionName = myClient.currentSession().name();
    sessionId = myClient.currentSession().id();
    runOnUiThread(new Runnable()
    {

      @Override
      public void run()
      {
        showToast("Session Joined");
        connectButton.setText(sessionName);
      }
    });
  }

  @Override
  public void onReceiveSessionList(List<CollabrifySession> sessionList)
  {
    if( sessionList.isEmpty() )
    {
      showToast("No session available");
      return;
    }
    displaySessionList(sessionList);
  }

  @Override
  public void onDisconnect()
  {
    runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        showToast("Left session");
        connectButton.setText("CreateSession");
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
    broadcastText.addTextChangedListener(new TextWatcher(){

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// TODO Auto-generated method stub
			if (count == 1){ //typing in the middle
				Log.d("KEY_EVENT", s.toString());//.substring(start, start+count-1));	
			}
			else{ //typing at end
				Log.d("KEY_EVENT", s.toString());//.substring(before, count-1));	
			}
			Log.d("KEY_EVENT", "start: " + start + " before: " + before + " count: " + count);
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
    	
    	
    });

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
  
  public void undo(View v)
  {
	  //TODO: Implement undo
  }
  
  public void redo(View v)
  {
	  //TODO: Implement redo
  }
  
  public void doBroadcast(View v)
  {
    if( broadcastText.getText().toString().isEmpty() )
      return;
    if( myClient != null && myClient.inSession() )
    {
      try
      {
    	// Create and serialize textAction with location and text
    	TextAction broadcastChar = new TextAction();
    	broadcastChar.location = broadcastText.getSelectionEnd();
    	
        myClient.broadcast(broadcastText.getText().toString().getBytes(),
            "lol", broadcastListener);
        //broadcastText.getText().clear();
      }
      catch( CollabrifyException e )
      {
        onError(e);
      }
    }
  }

  public void doCreateSession(View v)
  {
    try
    {
      Random rand = new Random();
      sessionName = "Test " + rand.nextInt(Integer.MAX_VALUE);
      myClient.createSession(sessionName, tags, password, 0,
          createSessionListener, sessionListener);
    }
    catch( CollabrifyException e )
    {
      onError(e);
    }
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
    for( CollabrifySession s : sessionList )
    {
      sessionNames.add(s.name());
    }

    // create a dialog to show the list of session names to the user
    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
	
  private static String toUnicode(char ch) {
	  return String.format("\\u%04x", (int) ch);
  }
}
