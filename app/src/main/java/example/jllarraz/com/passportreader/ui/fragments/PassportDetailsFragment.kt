package example.jllarraz.com.passportreader.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.common.IntentData
import example.jllarraz.com.passportreader.data.Passport
import example.jllarraz.com.passportreader.databinding.FragmentPassportDetailsBinding
import example.jllarraz.com.passportreader.utils.StringUtils
import org.jmrtd.FeatureStatus
import org.jmrtd.VerificationStatus
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.security.auth.x500.X500Principal

class PassportDetailsFragment : Fragment(R.layout.fragment_passport_details) {

    private var passportDetailsFragmentListener: PassportDetailsFragmentListener? = null

    private var simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    private var passport: Passport? = null

    private lateinit var binding: FragmentPassportDetailsBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPassportDetailsBinding.bind(view)

        val arguments = arguments
        if (arguments!!.containsKey(IntentData.KEY_PASSPORT)) {
            passport = arguments.getParcelable<Passport>(IntentData.KEY_PASSPORT)
        }

        binding.iconPhoto.setOnClickListener {
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

        binding.apply {
            if (passport.face != null) {
                //Add teh face
                iconPhoto.setImageBitmap(passport.face)
            } else if (passport.portrait != null) {
                //If we don't have the face, we try with the portrait
                iconPhoto.setImageBitmap(passport.portrait)
            }

            val personDetails = passport.personDetails
            if (personDetails != null) {
                val name = personDetails.primaryIdentifier!!.replace("<", "")
                val surname = personDetails.secondaryIdentifier!!.replace("<", "")
                valueName.text = getString(R.string.name, name, surname)
                valueDOB.text = personDetails.dateOfBirth
                valueGender.text = personDetails.gender!!.name
                valuePassportNumber.text = personDetails.documentNumber
                valueExpirationDate.text = personDetails.dateOfExpiry
                valueIssuingState.text = personDetails.issuingState
                valueNationality.text = personDetails.nationality
            }

            val additionalPersonDetails = passport.additionalPersonDetails
            if (additionalPersonDetails != null) {
                //This object it's not available in the majority of passports
                cardViewAdditionalPersonInformation.visibility = View.VISIBLE

                if (additionalPersonDetails.custodyInformation != null) {
                    valueCustody.text = additionalPersonDetails.custodyInformation
                }
                if (additionalPersonDetails.fullDateOfBirth != null) {

                    valueDateOfBirth.text = additionalPersonDetails.fullDateOfBirth
                }
                if (additionalPersonDetails.otherNames != null && additionalPersonDetails.otherNames!!.isNotEmpty()) {
                    valueOtherNames.text = arrayToString(additionalPersonDetails.otherNames!!)
                }
                if (additionalPersonDetails.otherValidTDNumbers != null && additionalPersonDetails.otherValidTDNumbers!!.isNotEmpty()) {
                    valueOtherTdNumbers.text = arrayToString(additionalPersonDetails.otherValidTDNumbers!!)
                }
                if (additionalPersonDetails.permanentAddress != null && additionalPersonDetails.permanentAddress!!.isNotEmpty()) {
                    valuePermanentAddress.text = arrayToString(additionalPersonDetails.permanentAddress!!)
                }

                if (additionalPersonDetails.personalNumber != null) {
                    valuePersonalNumber.text = additionalPersonDetails.personalNumber
                }

                if (additionalPersonDetails.personalSummary != null) {
                    valuePersonalSummary.text = additionalPersonDetails.personalSummary
                }

                if (additionalPersonDetails.placeOfBirth != null && additionalPersonDetails.placeOfBirth!!.isNotEmpty()) {
                    valuePlaceOfBirth.text = arrayToString(additionalPersonDetails.placeOfBirth!!)
                }

                if (additionalPersonDetails.profession != null) {
                    valueProfession.text = additionalPersonDetails.profession
                }

                if (additionalPersonDetails.telephone != null) {
                    valueTelephone.text = additionalPersonDetails.telephone
                }

                if (additionalPersonDetails.title != null) {
                    valueTitle.text = additionalPersonDetails.title
                }
            } else {
                cardViewAdditionalPersonInformation.visibility = View.GONE
            }

            val additionalDocumentDetails = passport.additionalDocumentDetails
            if (additionalDocumentDetails != null) {
                cardViewAdditionalDocumentInformation.visibility = View.VISIBLE

                if (additionalDocumentDetails.dateAndTimeOfPersonalization != null) {
                    valueDatePersonalization.text = additionalDocumentDetails.dateAndTimeOfPersonalization
                }
                if (additionalDocumentDetails.dateOfIssue != null) {
                    valueDateIssue.text = additionalDocumentDetails.dateOfIssue
                }

                if (additionalDocumentDetails.endorsementsAndObservations != null) {
                    valueEndorsements.text = additionalDocumentDetails.endorsementsAndObservations
                }

                if (additionalDocumentDetails.endorsementsAndObservations != null) {
                    valueEndorsements.text = additionalDocumentDetails.endorsementsAndObservations
                }

                if (additionalDocumentDetails.issuingAuthority != null) {
                    valueIssuingAuthority.text = additionalDocumentDetails.issuingAuthority
                }

                if (additionalDocumentDetails.namesOfOtherPersons != null) {
                    valueNamesOtherPersons.text = arrayToString(additionalDocumentDetails.namesOfOtherPersons!!)
                }

                if (additionalDocumentDetails.personalizationSystemSerialNumber != null) {
                    valueSystemSerialNumber.text = additionalDocumentDetails.personalizationSystemSerialNumber
                }

                if (additionalDocumentDetails.taxOrExitRequirements != null) {
                    valueTaxExit.text = additionalDocumentDetails.taxOrExitRequirements
                }
            } else {
                cardViewAdditionalDocumentInformation.visibility = View.GONE
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
                    valueDocumentSigningCertificateSerialNumber.text = docSigningCertificate.serialNumber.toString()
                    valueDocumentSigningCertificatePublicKeyAlgorithm.text = docSigningCertificate.publicKey.algorithm
                    valueDocumentSigningCertificateSignatureAlgorithm.text = docSigningCertificate.sigAlgName

                    try {
                        valueDocumentSigningCertificateThumbprint.text = StringUtils.bytesToHex(MessageDigest.getInstance("SHA-1").digest(
                                docSigningCertificate.encoded)).toUpperCase()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    valueDocumentSigningCertificateIssuer.text = docSigningCertificate.issuerDN.name
                    valueDocumentSigningCertificateSubject.text = docSigningCertificate.subjectDN.name
                    valueDocumentSigningCertificateValidFrom.text = simpleDateFormat.format(docSigningCertificate.notBefore)
                    valueDocumentSigningCertificateValidTo.text = simpleDateFormat.format(docSigningCertificate.notAfter)
                } else {
                    cardViewDocumentSigningCertificate.visibility = View.GONE
                }

            } else {
                cardViewDocumentSigningCertificate.visibility = View.GONE
            }
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
        binding.apply {
            cardViewWarning.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorCard))
            textWarningTitle.text = title
            textWarningMessage.text = message
        }
    }


    private fun displayAuthenticationStatus(verificationStatus: VerificationStatus?, featureStatus: FeatureStatus) {
        binding.apply {
            if (featureStatus.hasBAC() == FeatureStatus.Verdict.PRESENT) {
                rowBac.visibility = View.VISIBLE
            } else {
                rowBac.visibility = View.GONE
            }

            if (featureStatus.hasAA() == FeatureStatus.Verdict.PRESENT) {
                rowActive.visibility = View.VISIBLE
            } else {
                rowActive.visibility = View.GONE
            }

            if (featureStatus.hasSAC() == FeatureStatus.Verdict.PRESENT) {
                rowPace.visibility = View.VISIBLE
            } else {
                rowPace.visibility = View.GONE
            }

            if (featureStatus.hasCA() == FeatureStatus.Verdict.PRESENT) {
                rowChip.visibility = View.VISIBLE
            } else {
                rowChip.visibility = View.GONE
            }

            if (featureStatus.hasEAC() == FeatureStatus.Verdict.PRESENT) {
                rowEac.visibility = View.VISIBLE
            } else {
                rowEac.visibility = View.GONE
            }

            displayVerificationStatusIcon(valueBac, verificationStatus!!.bac)
            displayVerificationStatusIcon(valuePace, verificationStatus.sac)
            displayVerificationStatusIcon(valuePassive, verificationStatus.ht)
            displayVerificationStatusIcon(valueActive, verificationStatus.aa)
            displayVerificationStatusIcon(valueDocumentSigning, verificationStatus.ds)
            displayVerificationStatusIcon(valueCountrySigning, verificationStatus.cs)
            displayVerificationStatusIcon(valueChip, verificationStatus.ca)
            displayVerificationStatusIcon(valueEac, verificationStatus.eac)
        }
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
        imageView.setColorFilter(ContextCompat.getColor(requireActivity(), resourceColorId), android.graphics.PorterDuff.Mode.SRC_IN)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is PassportDetailsFragmentListener) {
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
