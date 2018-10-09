package example.jllarraz.com.passportreader.ui.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;

import net.sf.scuba.data.Gender;


import org.jmrtd.lds.icao.MRZInfo;

import java.util.List;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.common.IntentData;
import example.jllarraz.com.passportreader.ui.validators.DateRule;
import example.jllarraz.com.passportreader.ui.validators.DocumentNumberRule;

public class SelectionFragment extends Fragment implements Validator.ValidationListener {

    private RadioGroup radioGroup;
    private LinearLayout linearLayoutManual;
    private LinearLayout linearLayoutAutomatic;
    private AppCompatEditText appCompatEditTextDocumentNumber;
    private AppCompatEditText appCompatEditTextDocumentExpiration;
    private AppCompatEditText appCompatEditTextDateOfBirth;
    private Button buttonReadNFC;

    private Validator mValidator;
    private SelectionFragmentListener selectionFragmentListener;

    public static PassportDetailsFragment newInstance(MRZInfo mrzInfo, Bitmap face) {
        PassportDetailsFragment myFragment = new PassportDetailsFragment();
        Bundle args = new Bundle();
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View inflatedView = inflater.inflate(R.layout.fragment_selection, container, false);

        radioGroup = inflatedView.findViewById(R.id.radioButtonDataEntry);
        linearLayoutManual = inflatedView.findViewById(R.id.layoutManual);
        linearLayoutAutomatic = inflatedView.findViewById(R.id.layoutAutomatic);
        appCompatEditTextDocumentNumber = inflatedView.findViewById(R.id.documentNumber);
        appCompatEditTextDocumentExpiration = inflatedView.findViewById(R.id.documentExpiration);
        appCompatEditTextDateOfBirth = inflatedView.findViewById(R.id.documentDateOfBirth);
        buttonReadNFC = inflatedView.findViewById(R.id.buttonReadNfc);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.radioButtonManual:{
                        linearLayoutManual.setVisibility(View.VISIBLE);
                        linearLayoutAutomatic.setVisibility(View.GONE);
                        break;
                    }
                    case R.id.radioButtonOcr:{
                        linearLayoutManual.setVisibility(View.GONE);
                        linearLayoutAutomatic.setVisibility(View.VISIBLE);
                        if(selectionFragmentListener!=null){
                            selectionFragmentListener.onMrzRequest();
                        }
                        break;
                    }
                }
            }
        });

        buttonReadNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateFields();
            }
        });

        mValidator = new Validator(this);
        mValidator.setValidationListener(this);

        mValidator.put(appCompatEditTextDocumentNumber, new DocumentNumberRule());
        mValidator.put(appCompatEditTextDocumentExpiration, new DateRule());
        mValidator.put(appCompatEditTextDateOfBirth, new DateRule());

        return inflatedView;
    }

    protected void validateFields(){
        try {
            mValidator.removeRules(appCompatEditTextDocumentNumber);
            mValidator.removeRules(appCompatEditTextDocumentExpiration);
            mValidator.removeRules(appCompatEditTextDateOfBirth);

            mValidator.put(appCompatEditTextDocumentNumber, new DocumentNumberRule());
            mValidator.put(appCompatEditTextDocumentExpiration, new DateRule());
            mValidator.put(appCompatEditTextDateOfBirth, new DateRule());
        }catch (Exception e){
            e.printStackTrace();
        }
        mValidator.validate();
    }

    public void selectManualToggle(){
        radioGroup.check(R.id.radioButtonManual);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if(activity instanceof SelectionFragment.SelectionFragmentListener){
            selectionFragmentListener = (SelectionFragment.SelectionFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        selectionFragmentListener = null;
        super.onDetach();

    }


    @Override
    public void onValidationSucceeded() {

        String documentNumber = appCompatEditTextDocumentNumber.getText().toString();
        String dateOfBirth = appCompatEditTextDateOfBirth.getText().toString();
        String documentExpiration = appCompatEditTextDocumentExpiration.getText().toString();

        MRZInfo mrzInfo =  new MRZInfo("P",
                "ESP",
                "DUMMY",
                "DUMMY",
                documentNumber,
                "ESP",
                dateOfBirth,
                Gender.MALE,
                documentExpiration,
                "DUMMY"
                );
        if(selectionFragmentListener!=null){
            selectionFragmentListener.onPassportRead(mrzInfo);
        }
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(getContext());

            // Display error messages ;)
            if (view instanceof EditText) {
                ((EditText) view).setError(message);
            } else {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Listener
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    public interface SelectionFragmentListener {
        void onPassportRead(MRZInfo mrzInfo);
        void onMrzRequest();
    }
}
