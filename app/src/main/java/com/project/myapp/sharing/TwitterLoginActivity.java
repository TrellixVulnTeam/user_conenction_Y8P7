package com.project.myapp.sharing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.project.myapp.Bl_Settings;
import com.project.myapp.R;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class TwitterLoginActivity extends Activity implements OnClickListener {

	/* Shared preference keys */
	private static final String PREF_NAME = "sample_twitter_pref";
	private static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
	private static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
	private static final String PREF_KEY_TWITTER_LOGIN = "is_twitter_loggedin";
	private static final String PREF_USER_NAME = "twitter_user_name";
	private static final String PREF_USER_IMAGE = "twitter_user_image";

	/* Any number for uniquely distinguish your request */
	public static final int WEBVIEW_REQUEST_CODE = 100;

	private ProgressDialog pDialog;

	private static Twitter twitter;
	private static RequestToken requestToken;

	private static SharedPreferences mSharedPreferences;

	private TextView mShareEditText, userName;
	ImageView imageView, icon;

	private View shareLayout;

	private String consumerKey = null;
	private String consumerSecret = null;
	private String callbackUrl = null;
	private String oAuthVerifier = null;

	String stitle, sdiscription;

	// svideolink;
	// Bitmap simagelink;
	ImageLoader imageLoader = ImageLoader.getInstance();

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* initializing twitter parameters from string.xml */
		initTwitterConfigs();

		/* Enabling strict mode */
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		/* Setting activity layout file */
		setContentView(R.layout.twitter_login);

		imageView = (ImageView) findViewById(R.id.imageView);
		icon = (ImageView) findViewById(R.id.icon);
		shareLayout = (LinearLayout) findViewById(R.id.share_layout);
		mShareEditText = (TextView) findViewById(R.id.share_text);
		userName = (TextView) findViewById(R.id.user_name);

		/* register button click listeners */
		findViewById(R.id.btn_share).setOnClickListener(this);

		/* Check if required twitter keys are set */
		if (TextUtils.isEmpty(consumerKey) || TextUtils.isEmpty(consumerSecret)) {
			Toast.makeText(this, "Twitter key and secret not configured", Toast.LENGTH_SHORT).show();
			return;
		}

		findViewById(R.id.btn_cancel).setOnClickListener(this);

		/* Initialize application preferences */
		mSharedPreferences = getSharedPreferences(PREF_NAME, 0);

		boolean isLoggedIn = mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);

		imageLoader.init(GetOption.getConfig(getApplicationContext()));
		/* if already logged in, then hide login layout and show share layout */
		if (isLoggedIn) {

			shareLayout.setVisibility(View.VISIBLE);
			String username = mSharedPreferences.getString(PREF_USER_NAME, "");
			String userimage = mSharedPreferences.getString(PREF_USER_IMAGE, "");

			userName.setText(username);

			imageLoader.displayImage(userimage, icon, GetOption.getOption(), new ImageLoadingListener() {

				@Override
				public void onLoadingStarted(String arg0, View arg1) {
					System.out.println("in process");
				}

				@Override
				public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {
					System.out.println("in fail");
				}

				@Override
				public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {

				}

				@Override
				public void onLoadingCancelled(String arg0, View arg1) {
					System.out.println("in cancelled");
				}
			});

		} else {

			shareLayout.setVisibility(View.GONE);

			Uri uri = getIntent().getData();

			if (uri != null && uri.toString().startsWith(callbackUrl)) {

				String verifier = uri.getQueryParameter(oAuthVerifier);

				try {

					/* Getting oAuth authentication token */
					AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

					/* Getting user id form access token */
					long userID = accessToken.getUserId();
					final User user = twitter.showUser(userID);
					final String username = user.getName();
					/* save updated token */
					saveTwitterInfo(accessToken);

					shareLayout.setVisibility(View.VISIBLE);
					userName.setText(username);

					// imageLoader.displayImage(url, icon,
					// GetOption.getOption(),
					// new ImageLoadingListener() {
					//
					// @Override
					// public void onLoadingStarted(String arg0,
					// View arg1) {
					// System.out.println("in process");
					// }
					//
					// @Override
					// public void onLoadingFailed(String arg0,
					// View arg1, FailReason arg2) {
					// System.out.println("in fail");
					// }
					//
					// @Override
					// public void onLoadingComplete(String arg0,
					// View arg1, Bitmap arg2) {
					//
					// }
					//
					// @Override
					// public void onLoadingCancelled(String arg0,
					// View arg1) {
					// System.out.println("in cancelled");
					// }
					// });

				} catch (Exception e) {
					Log.e("Failed to login Twitter", e.getMessage());
				}
			}

		}
		loginToTwitter();

		Intent i = getIntent();

		if (i != null) {
			stitle = i.getStringExtra("title");
			// stitle = "Hello";
			// byte[] byteArray = i.getByteArrayExtra("imagelink");
			sdiscription = i.getStringExtra("discription");
			// simagelink = BitmapFactory.decodeByteArray(byteArray, 0,
			// byteArray.length);
		}
		// imageView.setImageBitmap(simagelink);
		imageView.setVisibility(View.GONE);
		mShareEditText.setText("Try MyApp for your smartphone. Download it here! https://play.google.com/store");
		
	}

	/**
	 * Saving user information, after user is authenticated for the first time.
	 * You don't need to show user to login, until user has a valid access toen
	 */
	private void saveTwitterInfo(AccessToken accessToken) {

		long userID = accessToken.getUserId();

		User user;
		try {
			user = twitter.showUser(userID);

			String username = user.getName();
			String url = user.getOriginalProfileImageURL();

			/* Storing oAuth tokens to shared preferences */
			Editor e = mSharedPreferences.edit();
			e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
			e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
			e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
			e.putString(PREF_USER_NAME, username);
			e.putString(PREF_USER_IMAGE, url);
			e.commit();

		} catch (TwitterException e1) {
			e1.printStackTrace();
		}
	}

	/* Reading twitter essential configuration parameters from strings.xml */
	private void initTwitterConfigs() {
		consumerKey = getString(R.string.twitter_consumer_key);
		consumerSecret = getString(R.string.twitter_consumer_secret);
		callbackUrl = getString(R.string.twitter_callback);
		oAuthVerifier = getString(R.string.twitter_oauth_verifier);
	}

	private void loginToTwitter() {
		boolean isLoggedIn = mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);

		if (!isLoggedIn) {
			final ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(consumerKey);
			builder.setOAuthConsumerSecret(consumerSecret);

			final Configuration configuration = builder.build();
			final TwitterFactory factory = new TwitterFactory(configuration);
			twitter = factory.getInstance();

			try {
				requestToken = twitter.getOAuthRequestToken(callbackUrl);

				/**
				 * Loading twitter login page on webview for authorization Once
				 * authorized, results are received at onActivityResult
				 * */
				final Intent intent = new Intent(this, WebViewActivity.class);
				intent.putExtra(WebViewActivity.EXTRA_URL, requestToken.getAuthenticationURL());
				startActivityForResult(intent, WEBVIEW_REQUEST_CODE);

			} catch (TwitterException e) {
				e.printStackTrace();
			}
		} else {

			shareLayout.setVisibility(View.VISIBLE);
		}
	}

	@SuppressWarnings("unused")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_OK) {
			String verifier = data.getExtras().getString(oAuthVerifier);
			try {
				AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

				long userID = accessToken.getUserId();
				final User user = twitter.showUser(userID);
				String username = user.getName();
				String url = user.getOriginalProfileImageURL();

				saveTwitterInfo(accessToken);

				shareLayout.setVisibility(View.VISIBLE);
				userName.setText(username);

				// imageLoader.displayImage(url, icon, GetOption.getOption(),
				// new ImageLoadingListener() {
				//
				// @Override
				// public void onLoadingStarted(String arg0, View arg1) {
				// System.out.println("in process");
				// }
				//
				// @Override
				// public void onLoadingFailed(String arg0, View arg1,
				// FailReason arg2) {
				// System.out.println("in fail");
				// }
				//
				// @Override
				// public void onLoadingComplete(String arg0,
				// View arg1, Bitmap arg2) {
				//
				// }
				//
				// @Override
				// public void onLoadingCancelled(String arg0,
				// View arg1) {
				// System.out.println("in cancelled");
				// }
				// });

			} catch (Exception e) {
				Log.e("Twitter Login Failed", e.getMessage());
			}
		}
		if (resultCode == 0) {
			TwitterLoginActivity.this.finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.btn_cancel:
			TwitterLoginActivity.this.finish();
			break;
		case R.id.btn_share:

			if (stitle.trim().length() > 0) {
				new updateTwitterStatus().execute(stitle);

			} else {
				Toast.makeText(this, "Message is empty!!", Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	class updateTwitterStatus extends AsyncTask<String, String, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			pDialog = new ProgressDialog(TwitterLoginActivity.this);
			pDialog.setMessage("Posting to twitter...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		protected Void doInBackground(String... args) {

			String status = args[0];
			try {
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(consumerKey);
				builder.setOAuthConsumerSecret(consumerSecret);

				// Access Token
				String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
				// Access Token Secret
				String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");

				AccessToken accessToken = new AccessToken(access_token, access_token_secret);
				Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

				// ByteArrayOutputStream stream = new ByteArrayOutputStream();
				// simagelink.compress(CompressFormat.JPEG, 100, stream);
				// InputStream is = new
				// ByteArrayInputStream(stream.toByteArray());

				// Update status
				StatusUpdate statusUpdate = new StatusUpdate(status);

				// statusUpdate.setMedia("test.jpg", is);

				twitter4j.Status response = twitter.updateStatus(statusUpdate);

				Log.d("Status", response.getText());

			} catch (TwitterException e) {
				Log.d("Failed to post!", e.getMessage());
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			/* Dismiss the progress dialog after sharing */
			pDialog.dismiss();

			Toast.makeText(TwitterLoginActivity.this, "Posted to Twitter!", Toast.LENGTH_SHORT).show();
			// Clearing EditText field
			mShareEditText.setText("");
			TwitterLoginActivity.this.finish();
		}

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		TwitterLoginActivity.this.finish();
	}
	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
		switch (keycode) {
			case KeyEvent.KEYCODE_MENU:
				// doSomething();
				// Toast.makeText(getApplicationContext(), "Menu Button Pressed",
				// Toast.LENGTH_SHORT).show();
				try {
					Intent i = new Intent(this, Bl_Settings.class);
					startActivity(i);
				} catch (Exception dd) {

				}

				return true;
		}

		return super.onKeyDown(keycode, e);
	}
}