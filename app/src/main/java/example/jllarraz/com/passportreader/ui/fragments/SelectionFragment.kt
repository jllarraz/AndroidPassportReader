package example.jllarraz.com.passportreader.ui.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.databinding.FragmentSelectionBinding
import example.jllarraz.com.passportreader.network.MasterListService
import example.jllarraz.com.passportreader.ui.validators.DateRule
import example.jllarraz.com.passportreader.ui.validators.DocumentNumberRule
import example.jllarraz.com.passportreader.utils.KeyStoreUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import net.sf.scuba.data.Gender
import org.jmrtd.lds.icao.MRZInfo
import java.security.Security
import java.security.cert.Certificate

class SelectionFragment : Fragment(R.layout.fragment_selection), Validator.ValidationListener {

    private var mValidator: Validator? = null
    private var selectionFragmentListener: SelectionFragmentListener? = null
    var disposable = CompositeDisposable()

    private lateinit var binding: FragmentSelectionBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSelectionBinding.bind(view)

        binding.apply {
            radioButtonDataEntry.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radioButtonManual -> {
                        layoutManual.visibility = View.VISIBLE
                        layoutAutomatic.visibility = View.GONE
                    }
                    R.id.radioButtonOcr -> {
                        layoutManual.visibility = View.GONE
                        layoutAutomatic.visibility = View.VISIBLE
                        if (selectionFragmentListener != null) {
                            selectionFragmentListener!!.onMrzRequest()
                        }
                    }
                }
            }


            buttonReadNfc.setOnClickListener { validateFields() }

        mValidator = Validator(this)
        mValidator!!.setValidationListener(this)

            mValidator!!.put(documentNumber, DocumentNumberRule())
            mValidator!!.put(documentExpiration, DateRule())
            mValidator!!.put(documentDateOfBirth, DateRule())

            buttonDownloadCSCA.setOnClickListener { requireDownloadCSCA() }
            buttonDeleteCSCA.setOnClickListener {
                val subscribe = cleanCSCAFolder()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { _ ->
                            Toast.makeText(requireContext(), "CSCA Folder deleted", Toast.LENGTH_SHORT).show()
                        }
                disposable.add(subscribe)
            }
        }
    }

    private fun validateFields() {
        try {
            binding.apply {
                mValidator!!.removeRules(documentNumber)
                mValidator!!.removeRules(documentExpiration)
                mValidator!!.removeRules(documentDateOfBirth)

                mValidator!!.put(documentNumber, DocumentNumberRule())
                mValidator!!.put(documentExpiration, DateRule())
                mValidator!!.put(documentDateOfBirth, DateRule())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mValidator!!.validate()
    }

    fun selectManualToggle() {
        binding.radioButtonDataEntry.check(R.id.radioButtonManual)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is SelectionFragmentListener) {
            selectionFragmentListener = activity
        }
    }

    override fun onDetach() {
        selectionFragmentListener = null
        super.onDetach()

    }

    override fun onDestroyView() {

        if (!disposable.isDisposed) {
            disposable.dispose()
        }
        super.onDestroyView()
    }


    override fun onValidationSucceeded() {

        binding.apply {
            val documentNumber = documentNumber.text!!.toString()
            val dateOfBirth = documentDateOfBirth.text!!.toString()
            val documentExpiration = documentExpiration.text!!.toString()

            val mrzInfo = MRZInfo("P",
                    "ESP",
                    "DUMMY",
                    "DUMMY",
                    documentNumber,
                    "ESP",
                    dateOfBirth,
                    Gender.MALE,
                    documentExpiration,
                    "DUMMY"
            )
            if (selectionFragmentListener != null) {
                selectionFragmentListener!!.onPassportRead(mrzInfo)
            }
        }
    }

    override fun onValidationFailed(errors: List<ValidationError>) {
        for (error in errors) {
            val view = error.view
            val message = error.getCollatedErrorMessage(context)

            // Display error messages ;)
            if (view is EditText) {
                view.error = message
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Listener
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    interface SelectionFragmentListener {
        fun onPassportRead(mrzInfo: MRZInfo)
        fun onMrzRequest()
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Download Master List Spanish Certificates
    //
    ////////////////////////////////////////////////////////////////////////////////////////


    fun requireDownloadCSCA() {
        val downloadsFolder = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
        val keyStore = KeyStoreUtils().readKeystoreFromFile(downloadsFolder)
        if (keyStore == null || keyStore.aliases().toList().isNullOrEmpty()) {
            //No certificates downloaded
            downloadSpanishMasterList()
        } else {
            //Certificates in the keystore
            val dialog = AlertDialog.Builder(requireContext())
                    .setTitle(R.string.keystore_not_empty_title)
                    .setMessage(R.string.keystore_not_empty_message)
                    .setPositiveButton(android.R.string.ok, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            val subscribe = cleanCSCAFolder()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe { result ->
                                        downloadSpanishMasterList()
                                    }
                            disposable.add(subscribe)

                        }
                    })
                    .setNegativeButton(android.R.string.cancel, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            //WE don't do anything
                        }

                    })
                    .create()
            dialog.show()
        }
    }

    fun cleanCSCAFolder(): Single<Boolean> {
        return Single.fromCallable {
            try {
                val downloadsFolder = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                val listFiles = downloadsFolder.listFiles()
                for (tempFile in listFiles) {
                    tempFile.delete()
                }
                downloadsFolder.listFiles()
                true
            }catch (e:java.lang.Exception){
                false
            }
        }
    }

    fun downloadSpanishMasterList() {
        val masterListService = MasterListService(requireContext(), "https://www.dnielectronico.es/")
        val subscribe = masterListService.getSpanishMasterList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { certificates ->
                            saveCertificates(certificates)
                        },
                        {error->
                            Toast.makeText(requireContext(), "No certificates has been download "+error, Toast.LENGTH_SHORT).show()
                        }
                )
        disposable.add(subscribe)
    }

    fun saveCertificates(certificates: ArrayList<Certificate>) {
        val subscribe = Single.fromCallable {
            try {
                val size = certificates.size
                Log.d(TAG, "Number of certificates: " + size)
                val map = KeyStoreUtils().toMap(certificates)
                val downloadsFolder = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                KeyStoreUtils().toKeyStoreFile(map, outputDir = downloadsFolder)
                size
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                -1
            }
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { result ->
                    if(result>0) {
                        Toast.makeText(requireContext(), "Certificates Downloaded: "+result, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "No certificates has been download", Toast.LENGTH_SHORT).show()
                    }
                }
        disposable.add(subscribe)
    }

    companion object {
        val TAG = SelectionFragment::class.java.simpleName
        init {
            Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        }
    }
}
