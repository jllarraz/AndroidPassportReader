package example.jllarraz.com.passportreader.ui.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.common.IntentData;
import example.jllarraz.com.passportreader.data.Passport;
import example.jllarraz.com.passportreader.ui.views.TouchImageView;

public class PassportPhotoFragment extends Fragment{

    private PassportPhotoFragmentListener passportPhotoFragmentListener;

    private Bitmap bitmap;

    private TouchImageView appCompatImageViewFace;

    public static PassportPhotoFragment newInstance(Bitmap bitmap) {
        PassportPhotoFragment myFragment = new PassportPhotoFragment();
        Bundle args = new Bundle();
        args.putParcelable(IntentData.KEY_IMAGE, bitmap);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View inflatedView = inflater.inflate(R.layout.fragment_photo, container, false);

        Bundle arguments = getArguments();
        if(arguments.containsKey(IntentData.KEY_IMAGE)){
            bitmap = (Bitmap) arguments.getParcelable(IntentData.KEY_IMAGE);
        } else {
            //error
        }

        appCompatImageViewFace =  inflatedView.findViewById(R.id.image);

        return inflatedView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData(bitmap);
    }

    private void refreshData( Bitmap bitmap){
        if(bitmap == null){
            return;
        }
        appCompatImageViewFace.setImageBitmap(bitmap);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if(activity instanceof PassportPhotoFragmentListener){
            passportPhotoFragmentListener = (PassportPhotoFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        passportPhotoFragmentListener = null;
        super.onDetach();

    }

    public interface PassportPhotoFragmentListener {

    }

}
