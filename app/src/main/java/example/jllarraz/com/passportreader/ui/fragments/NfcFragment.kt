package example.jllarraz.com.passportreader.ui.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import net.sf.scuba.smartcards.CardServiceException
import net.sf.scuba.smartcards.ISO7816


import org.jmrtd.AccessDeniedException
import org.jmrtd.BACDeniedException
import org.jmrtd.PACEException
import org.jmrtd.lds.icao.MRZInfo
import org.spongycastle.jce.provider.BouncyCastleProvider


import java.security.Security


import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.asynctask.NfcPassportAsyncTask
import example.jllarraz.com.passportreader.common.IntentData
import example.jllarraz.com.passportreader.data.Passport


class NfcFragment : androidx.fragment.app.Fragment() {

    private var mrzInfo: MRZInfo? = null
    private var nfcFragmentListener: NfcFragmentListener? = null
    private var textViewPassportNumber: TextView? = null
    private var textViewDateOfBirth: TextView? = null
    private var textViewDateOfExpiry: TextView? = null
    private var progressBar: ProgressBar? = null

    internal var mHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val inflatedView = inflater.inflate(R.layout.fragment_nfc, container, false)

        return inflatedView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arguments = arguments
        if (arguments!!.containsKey(IntentData.KEY_MRZ_INFO)) {
            mrzInfo = arguments.getSerializable(IntentData.KEY_MRZ_INFO) as MRZInfo
        } else {
            //error
        }

        textViewPassportNumber = view.findViewById(R.id.value_passport_number)
        textViewDateOfBirth = view.findViewById(R.id.value_DOB)
        textViewDateOfExpiry = view.findViewById(R.id.value_expiration_date)
        progressBar = view.findViewById(R.id.progressBar)
    }

    fun handleNfcTag(intent: Intent?) {
        if (intent == null || intent.extras == null) {
            return
        }
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        onNFCSReadStart()
        val nfcPassportAsyncTask = NfcPassportAsyncTask(context!!.applicationContext, tag, mrzInfo!!, object : NfcPassportAsyncTask.NfcPassportAsyncTaskListener {
            override fun onPassportRead(passport: Passport?) {
                this@NfcFragment.onPassportRead(passport)
                onNFCReadFinish()
            }

            override fun onAccessDeniedException(exception: AccessDeniedException) {
                Toast.makeText(context, getString(R.string.warning_authentication_failed), Toast.LENGTH_SHORT).show()
                exception.printStackTrace()
                this@NfcFragment.onCardException(exception)
                onNFCReadFinish()
            }

            override fun onBACDeniedException(exception: BACDeniedException) {
                Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                this@NfcFragment.onCardException(exception)
                onNFCReadFinish()
            }

            override fun onPACEException(exception: PACEException) {
                Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                this@NfcFragment.onCardException(exception)
                onNFCReadFinish()
            }

            override fun onCardException(cardException: CardServiceException) {
                val sw = cardException.sw.toShort()
                when (sw) {
                    ISO7816.SW_CLA_NOT_SUPPORTED -> {
                        Toast.makeText(context, getString(R.string.warning_cla_not_supported), Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, cardException.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
                this@NfcFragment.onCardException(cardException)
                onNFCReadFinish()
            }

            override fun onGeneralException(exception: Exception?) {
                Toast.makeText(context, exception!!.toString(), Toast.LENGTH_SHORT).show()
                this@NfcFragment.onCardException(exception)
                onNFCReadFinish()
            }
        })
        nfcPassportAsyncTask.execute()

    }


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val activity = activity
        if (activity is NfcFragment.NfcFragmentListener) {
            nfcFragmentListener = activity
        }
    }

    override fun onDetach() {
        nfcFragmentListener = null
        super.onDetach()
    }


    override fun onResume() {
        super.onResume()

        textViewPassportNumber!!.text = getString(R.string.doc_number, mrzInfo!!.documentNumber)
        textViewDateOfBirth!!.text = getString(R.string.doc_dob, mrzInfo!!.dateOfBirth)
        textViewDateOfExpiry!!.text = getString(R.string.doc_expiry, mrzInfo!!.dateOfExpiry)

        if (nfcFragmentListener != null) {
            nfcFragmentListener!!.onEnableNfc()
        }
    }

    override fun onPause() {
        super.onPause()
        if (nfcFragmentListener != null) {
            nfcFragmentListener!!.onDisableNfc()
        }
    }

    protected fun onNFCSReadStart() {
        Log.d(TAG, "onNFCSReadStart")
        mHandler.post { progressBar!!.visibility = View.VISIBLE }

    }

    protected fun onNFCReadFinish() {
        Log.d(TAG, "onNFCReadFinish")
        mHandler.post { progressBar!!.visibility = View.GONE }
    }

    protected fun onCardException(cardException: Exception?) {
        mHandler.post {
            if (nfcFragmentListener != null) {
                nfcFragmentListener!!.onCardException(cardException)
            }
        }
    }

    protected fun onPassportRead(passport: Passport?) {
        mHandler.post {
            if (nfcFragmentListener != null) {
                nfcFragmentListener!!.onPassportRead(passport)
            }
        }
    }

    interface NfcFragmentListener {
        fun onEnableNfc()
        fun onDisableNfc()
        fun onPassportRead(passport: Passport?)
        fun onCardException(cardException: Exception?)
    }

    companion object {
        private val TAG = NfcFragment::class.java.simpleName

        init {
            Security.addProvider(BouncyCastleProvider())
        }

        fun newInstance(mrzInfo: MRZInfo): NfcFragment {
            val myFragment = NfcFragment()
            val args = Bundle()
            args.putSerializable(IntentData.KEY_MRZ_INFO, mrzInfo)
            myFragment.arguments = args
            return myFragment
        }
    }
}
