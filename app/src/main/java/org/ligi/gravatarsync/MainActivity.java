package org.ligi.gravatarsync;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends ActionBarActivity {

    @OnClick(R.id.syncNowButton)
    void syncClicked() {

        new Thread(new Runnable() {

            @Override
            public void run() {
                doSync();
            }
        }).start();

    }

    private void doSync() {
        final ContentResolver cr = getContentResolver();
        final Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if (cur.getCount() > 0) {


            final ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            while (cur.moveToNext()) {

                final String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                final String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                final Cursor emailCur = cr.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        new String[]{id}, null);

                while (emailCur.moveToNext()) {
                    String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));

                    Log.i("", "GotContact " + name + " " + email);
                }

                final Cursor cursor2 = getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, null, ContactsContract.RawContacts.CONTACT_ID + "=?", new String[]{String.valueOf(id)}, null);
                cursor2.moveToFirst();
                final long rawContactId = cursor2.getLong(cursor2.getColumnIndex(ContactsContract.RawContacts._ID));
                cursor2.close();

                final byte[] imageData = getGravatarImage("ligi@ligi.de");
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)

                                //.withSelection(ContactsContract.Data._ID+"=?", new String[]{""+rawContactId})
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, imageData).build());

            }

            try {
                final ContentProviderResult[] contentProviderResults = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                Log.i("","" + contentProviderResults.length);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }

        }
    }


    private byte[] getGravatarImage(final String email) {
        finagil OkHttpClient client = new OkHttpClient();

        final String url = "http://www.gravatar.com/avatar/" + getMD5String(email) + "?d=404";
        final Request request = new Request.Builder().url(url).build();

        try {
            final Response response = client.newCall(request).execute();


            if (response.code() == 200) {
                return ByteStreams.toByteArray(response.body().byteStream());
            }
        } catch (IOException e) {
        }
        return new byte[0];
    }


    public static String getMD5String(String from) {
        try {
            final MessageDigest mdEnc = MessageDigest.getInstance("MD5");
            mdEnc.update(from.getBytes(), 0, from.length());
            String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
            while (md5.length() < 32) {
                md5 = "0" + md5;
            }
            return md5;

        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception while encrypting to md5");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);
    }

}
