package sca.biwiwatchface.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/*
 * Implement AbstractAccountAuthenticator and stub out all
 * of its methods
 */
public class Authenticator extends AbstractAccountAuthenticator {

    private static final String TAG = Authenticator.class.getSimpleName();
    private Context mContext;

    // Simple constructor
    public Authenticator(Context context) {
        super(context);
        mContext = context;
    }

    // Editing properties is not supported
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse r, String s) {
        Log.d( TAG, "editProperties: " );
        throw new UnsupportedOperationException();
    }

    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "biwiwatchface.sca";
    // The account name
    public static final String ACCOUNT_NAME = "BiWi";

    public static Account getSyncAccount( Context context ) {
        // Create the account type and default account
        Account newAccount = new Account( ACCOUNT_NAME, ACCOUNT_TYPE );
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService( context.ACCOUNT_SERVICE );

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {
            /*
             * Add the account and account type, no password or user data
             * If successful, return the Account object, otherwise report an error.
             */
            if ( ! accountManager.addAccountExplicitly( newAccount, "", null ) ) {
                /*
                 * The account exists or some other error occurred. Log this, report it,
                 * or handle it internally.
                 */
                return null;
            }
        }
        return newAccount;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d( TAG, "addAccount: " + accountType );
        Account syncAccount = getSyncAccount( mContext );
        Bundle resBundle = new Bundle();
        if (syncAccount != null) {
            resBundle.putString( AccountManager.KEY_ACCOUNT_NAME, ACCOUNT_NAME );
            resBundle.putString( AccountManager.KEY_ACCOUNT_TYPE, accountType );
            SyncService.startSyncing( syncAccount );
        } else {
            resBundle.putInt( AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_CANCELED );
            resBundle.putString( AccountManager.KEY_ERROR_MESSAGE, "Could not create account" );
        }
        return resBundle;
    }

    // Ignore attempts to confirm credentials
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse r, Account account, Bundle bundle) throws NetworkErrorException {
        Log.d( TAG, "confirmCredentials: " );
        return null;
    }

    // Getting an authentication token is not supported
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse r, Account account, String s, Bundle bundle) throws NetworkErrorException {
        Log.d( TAG, "getAuthToken: " );
        throw new UnsupportedOperationException();
    }

    // Getting a label for the auth token is not supported
    @Override
    public String getAuthTokenLabel(String s) {
        Log.d( TAG, "getAuthTokenLabel: " );
        throw new UnsupportedOperationException();
    }

    // Updating user credentials is not supported
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse r, Account account, String s, Bundle bundle) throws NetworkErrorException {
        Log.d( TAG, "updateCredentials: " );
        throw new UnsupportedOperationException();
    }

    // Checking features for the account is not supported
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse r, Account account, String[] strings) throws NetworkErrorException {
        Log.d( TAG, "hasFeatures: " );
        throw new UnsupportedOperationException();
    }
}