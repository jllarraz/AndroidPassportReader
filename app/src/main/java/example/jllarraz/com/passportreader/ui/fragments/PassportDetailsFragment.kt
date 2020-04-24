package example.jllarraz.com.passportreader.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.jmrtd.FeatureStatus
import org.jmrtd.VerificationStatus

import java.security.MessageDigest
import java.text.SimpleDateFormat

import javax.security.auth.x500.X500Principal

import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.common.IntentData
import example.jllarraz.com.passportreader.data.Passport
import example.jllarraz.com.passportreader.utils.StringUtils
import kotlinx.android.synthetic.main.fragment_passport_details.*
import java.util.*

class PassportDetailsFragment : androidx.fragment.app.Fragment() {

    private var passportDetailsFragmentListener: PassportDetailsFragmentListener? = null

    internal var simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    private var passport: Passport? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val inflatedView = inflater.inflate(R.layout.fragment_passport_details, container, false)




        return inflatedView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arguments = arguments
        if (arguments!!.containsKey(IntentData.KEY_PASSPORT)) {
            passport = arguments.getParcelable<Passport>(IntentData.KEY_PASSPORT)
        } else {
            //error
        }


        iconPhoto!!.setOnClickListener {
            var bitmap = passport!!.face
            if (bitmap == null) {
                bitmap = passport!!.portrait
            }
            if (passportDetailsFragmentListener != null) {
                passportDetailsFragmentListener!!.onImageSelected(bitmap)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        refreshData(passport)
    }

    private fun refreshData(passport: Passport?) {
        if (passport == null) {
            return
        }

        if (passport.face != null) {
            //Add teh face
            iconPhoto!!.setImageBitmap(passport.face)
        } else if (passport.portrait != null) {
            //If we don't have the face, we try with the portrait
            iconPhoto!!.setImageBitmap(passport.portrait)
        }

        val personDetails = passport.personDetails
        if (personDetails != null) {
            val name = personDetails.primaryIdentifier!!.replace("<", "")
            val surname = personDetails.secondaryIdentifier!!.replace("<", "")
            value_name!!.text = getString(R.string.name, name, surname)
            value_DOB!!.text = personDetails.dateOfBirth
            value_gender!!.text = personDetails.gender!!.name
            value_passport_number!!.text = personDetails.documentNumber
            value_expiration_date!!.text = personDetails.dateOfExpiry
            value_issuing_state!!.text = personDetails.issuingState
            value_nationality!!.text = personDetails.nationality
        }

        val additionalPersonDetails = passport.additionalPersonDetails
        if (additionalPersonDetails != null) {
            //This object it's not available in the majority of passports
            card_view_additional_person_information!!.visibility = View.VISIBLE

            if (additionalPersonDetails.custodyInformation != null) {
                value_custody!!.text = additionalPersonDetails.custodyInformation
            }
            if (additionalPersonDetails.fullDateOfBirth != null) {

                value_date_of_birth!!.text = additionalPersonDetails.fullDateOfBirth
            }
            if (additionalPersonDetails.otherNames != null && additionalPersonDetails.otherNames!!.size > 0) {
                value_other_names!!.text = arrayToString(additionalPersonDetails.otherNames!!)
            }
            if (additionalPersonDetails.otherValidTDNumbers != null && additionalPersonDetails.otherValidTDNumbers!!.size > 0) {
                value_other_td_numbers!!.text = arrayToString(additionalPersonDetails.otherValidTDNumbers!!)
            }
            if (additionalPersonDetails.permanentAddress != null && additionalPersonDetails.permanentAddress!!.size > 0) {
                value_permanent_address!!.text = arrayToString(additionalPersonDetails.permanentAddress!!)
            }

            if (additionalPersonDetails.personalNumber != null) {
                value_personal_number!!.text = additionalPersonDetails.personalNumber
            }

            if (additionalPersonDetails.personalSummary != null) {
                value_personal_summary!!.text = additionalPersonDetails.personalSummary
            }

            if (additionalPersonDetails.placeOfBirth != null && additionalPersonDetails.placeOfBirth!!.size > 0) {
                value_place_of_birth!!.text = arrayToString(additionalPersonDetails.placeOfBirth!!)
            }

            if (additionalPersonDetails.profession != null) {
                value_profession!!.text = additionalPersonDetails.profession
            }

            if (additionalPersonDetails.telephone != null) {
                value_telephone!!.text = additionalPersonDetails.telephone
            }

            if (additionalPersonDetails.title != null) {
                value_title!!.text = additionalPersonDetails.title
            }
        } else {
            card_view_additional_person_information!!.visibility = View.GONE
        }

        val additionalDocumentDetails = passport.additionalDocumentDetails
        if (additionalDocumentDetails != null) {
            card_view_additional_document_information!!.visibility = View.VISIBLE

            if (additionalDocumentDetails.dateAndTimeOfPersonalization != null) {
                value_date_personalization!!.text = additionalDocumentDetails.dateAndTimeOfPersonalization
            }
            if (additionalDocumentDetails.dateOfIssue != null) {
                value_date_issue!!.text = additionalDocumentDetails.dateOfIssue
            }

            if (additionalDocumentDetails.endorsementsAndObservations != null) {
                value_endorsements!!.text = additionalDocumentDetails.endorsementsAndObservations
            }

            if (additionalDocumentDetails.endorsementsAndObservations != null) {
                value_endorsements!!.text = additionalDocumentDetails.endorsementsAndObservations
            }

            if (additionalDocumentDetails.issuingAuthority != null) {
                value_issuing_authority!!.text = additionalDocumentDetails.issuingAuthority
            }

            if (additionalDocumentDetails.namesOfOtherPersons != null) {
                value_names_other_persons!!.text = arrayToString(additionalDocumentDetails.namesOfOtherPersons!!)
            }

            if (additionalDocumentDetails.personalizationSystemSerialNumber != null) {
                value_system_serial_number!!.text = additionalDocumentDetails.personalizationSystemSerialNumber
            }

            if (additionalDocumentDetails.taxOrExitRequirements != null) {
                value_tax_exit!!.text = additionalDocumentDetails.taxOrExitRequirements
            }
        } else {
            card_view_additional_document_information!!.visibility = View.GONE
        }

        displayAuthenticationStatus(passport.verificationStatus, passport.featureStatus!!)
        displayWarningTitle(passport.verificationStatus, passport.featureStatus!!)


        val sodFile = passport.sodFile
        if (sodFile != null) {
            val countrySigningCertificate = sodFile.issuerX500Principal
            val dnRFC2253 = countrySigningCertificate.getName(X500Principal.RFC2253)
            val dnCANONICAL = countrySigningCertificate.getName(X500Principal.CANONICAL)
            val dnRFC1779 = countrySigningCertificate.getName(X500Principal.RFC1779)

            val name = countrySigningCertificate.name
            //new X509Certificate(countrySigningCertificate);

            val docSigningCertificate = sodFile.docSigningCertificate

            if (docSigningCertificate != null) {

                value_document_signing_certificate_serial_number!!.text = docSigningCertificate.serialNumber.toString()
                value_document_signing_certificate_public_key_algorithm!!.text = docSigningCertificate.publicKey.algorithm
                value_document_signing_certificate_signature_algorithm!!.text = docSigningCertificate.sigAlgName

                try {
                    value_document_signing_certificate_thumbprint!!.text = StringUtils.bytesToHex(MessageDigest.getInstance("SHA-1").digest(
                            docSigningCertificate.encoded)).toUpperCase()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                value_document_signing_certificate_issuer!!.text = docSigningCertificate.issuerDN.name
                value_document_signing_certificate_subject!!.text = docSigningCertificate.subjectDN.name
                value_document_signing_certificate_valid_from!!.text = simpleDateFormat.format(docSigningCertificate.notBefore)
                value_document_signing_certificate_valid_to!!.text = simpleDateFormat.format(docSigningCertificate.notAfter)

            } else {
                card_view_document_signing_certificate!!.visibility = View.GONE
            }

        } else {
            card_view_document_signing_certificate!!.visibility = View.GONE
        }
    }

    private fun displayWarningTitle(verificationStatus: VerificationStatus?, featureStatus: FeatureStatus) {
        var colorCard = android.R.color.holo_green_light
        var message = ""
        var title = ""
        if (featureStatus.hasCA() == FeatureStatus.Verdict.PRESENT) {
            if (verificationStatus!!.ca == VerificationStatus.Verdict.SUCCEEDED && verificationStatus.ht == VerificationStatus.Verdict.SUCCEEDED && verificationStatus.cs == VerificationStatus.Verdict.SUCCEEDED) {
                //Everything is fine
                colorCard = android.R.color.holo_green_light
                title = getString(R.string.document_valid_passport)
                message = getString(R.string.document_chip_content_success)
            } else if (verificationStatus.ca == VerificationStatus.Verdict.FAILED) {
                //Chip authentication failed
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_chip_failure)
            } else if (verificationStatus.ht == VerificationStatus.Verdict.FAILED) {
                //Document information
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_document_failure)
            } else if (verificationStatus.cs == VerificationStatus.Verdict.FAILED) {
                //CSCA information
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_csca_failure)
            } else {
                //Unknown
                colorCard = android.R.color.darker_gray
                title = getString(R.string.document_unknown_passport_title)
                message = getString(R.string.document_unknown_passport_message)
            }
        } else if (featureStatus.hasCA() == FeatureStatus.Verdict.NOT_PRESENT) {
            if (verificationStatus!!.ht == VerificationStatus.Verdict.SUCCEEDED) {
                //Document information is fine
                colorCard = android.R.color.holo_green_light
                title = getString(R.string.document_valid_passport)
                message = getString(R.string.document_content_success)
            } else if (verificationStatus.ht == VerificationStatus.Verdict.FAILED) {
                //Document information
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_document_failure)
            } else if (verificationStatus.cs == VerificationStatus.Verdict.FAILED) {
                //CSCA information
                colorCard = android.R.color.holo_red_light
                title = getString(R.string.document_invalid_passport)
                message = getString(R.string.document_csca_failure)
            } else {
                //Unknown
                colorCard = android.R.color.darker_gray
                title = getString(R.string.document_unknown_passport_title)
                message = getString(R.string.document_unknown_passport_message)
            }
        } else {
            //Unknown
            colorCard = android.R.color.darker_gray
            title = getString(R.string.document_unknown_passport_title)
            message = getString(R.string.document_unknown_passport_message)
        }
        card_view_warning!!.setCardBackgroundColor(resources.getColor(colorCard))
        textWarningTitle!!.text = title
        textWarningMessage!!.text = message
    }


    private fun displayAuthenticationStatus(verificationStatus: VerificationStatus?, featureStatus: FeatureStatus) {

        if (featureStatus.hasBAC() == FeatureStatus.Verdict.PRESENT) {
            row_bac!!.visibility = View.VISIBLE
        } else {
            row_bac!!.visibility = View.GONE
        }

        if (featureStatus.hasAA() == FeatureStatus.Verdict.PRESENT) {
            row_active!!.visibility = View.VISIBLE
        } else {
            row_active!!.visibility = View.GONE
        }

        if (featureStatus.hasSAC() == FeatureStatus.Verdict.PRESENT) {
            row_pace!!.visibility = View.VISIBLE
        } else {
            row_pace!!.visibility = View.GONE
        }

        if (featureStatus.hasCA() == FeatureStatus.Verdict.PRESENT) {
            row_chip!!.visibility = View.VISIBLE
        } else {
            row_chip!!.visibility = View.GONE
        }

        if (featureStatus.hasEAC() == FeatureStatus.Verdict.PRESENT) {
            row_eac!!.visibility = View.VISIBLE
        } else {
            row_eac!!.visibility = View.GONE
        }

        displayVerificationStatusIcon(value_bac, verificationStatus!!.bac)
        displayVerificationStatusIcon(value_pace, verificationStatus.sac)
        displayVerificationStatusIcon(value_passive, verificationStatus.ht)
        displayVerificationStatusIcon(value_active, verificationStatus.aa)
        displayVerificationStatusIcon(value_document_signing, verificationStatus.ds)
        displayVerificationStatusIcon(value_country_signing, verificationStatus.cs)
        displayVerificationStatusIcon(value_chip, verificationStatus.ca)
        displayVerificationStatusIcon(value_eac, verificationStatus.eac)
    }

    private fun displayVerificationStatusIcon(imageView: ImageView?, verdict: VerificationStatus.Verdict?) {
        var verdict = verdict
        if (verdict == null) {
            verdict = VerificationStatus.Verdict.UNKNOWN
        }
        val resourceIconId: Int
        val resourceColorId: Int
        when (verdict) {
            VerificationStatus.Verdict.SUCCEEDED -> {
                resourceIconId = R.drawable.ic_check_circle_outline
                resourceColorId = android.R.color.holo_green_light
            }
            VerificationStatus.Verdict.FAILED -> {
                resourceIconId = R.drawable.ic_close_circle_outline
                resourceColorId = android.R.color.holo_red_light
            }
            VerificationStatus.Verdict.NOT_PRESENT -> {
                resourceIconId = R.drawable.ic_close_circle_outline
                resourceColorId = android.R.color.darker_gray
            }
            VerificationStatus.Verdict.NOT_CHECKED -> {
                resourceIconId = R.drawable.ic_help_circle_outline
                resourceColorId = android.R.color.holo_orange_light
            }
            VerificationStatus.Verdict.UNKNOWN -> {
                resourceIconId = R.drawable.ic_close_circle_outline
                resourceColorId = android.R.color.darker_gray
            }
            else -> {
                resourceIconId = R.drawable.ic_close_circle_outline
                resourceColorId = android.R.color.darker_gray
            }
        }

        imageView!!.setImageResource(resourceIconId)
        imageView.setColorFilter(ContextCompat.getColor(activity!!, resourceColorId), android.graphics.PorterDuff.Mode.SRC_IN)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is PassportDetailsFragment.PassportDetailsFragmentListener) {
            passportDetailsFragmentListener = activity
        }
    }

    override fun onDetach() {
        passportDetailsFragmentListener = null
        super.onDetach()

    }

    interface PassportDetailsFragmentListener {
        fun onImageSelected(bitmap: Bitmap?)
    }


    private fun arrayToString(array: List<String>): String {
        var temp = ""
        val iterator = array.iterator()
        while (iterator.hasNext()) {
            temp += iterator.next() + "\n"
        }
        if (temp.endsWith("\n")) {
            temp = temp.substring(0, temp.length - "\n".length)
        }
        return temp
    }

    companion object {


        fun newInstance(passport: Passport): PassportDetailsFragment {
            val myFragment = PassportDetailsFragment()
            val args = Bundle()
            args.putParcelable(IntentData.KEY_PASSPORT, passport)
            myFragment.arguments = args
            return myFragment
        }
    }
}
