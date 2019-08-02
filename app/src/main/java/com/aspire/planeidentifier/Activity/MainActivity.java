package com.aspire.planeidentifier.Activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.MainThread;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.aspire.planeidentifier.R;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class MainActivity extends AppCompatActivity {
    private ArFragment arFragment;

    private ModelRenderable arrowRenderable, turnRenderable;
    private ViewRenderable welcomeViewRenderable;
    private ArSceneView arSceneView;
    private Session session;
    int countGo = 1, countRight = 1;
    private boolean sessionConfigured = false;
    private static final String TAG = MainActivity.class.getSimpleName();

    @UiThread
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arSceneView = arFragment.getArSceneView();
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
        //Hiding Plane Detection
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);
        arSceneView.getPlaneRenderer().setEnabled(false);
        ViewRenderable.builder()
                .setView(this, R.layout.activity_one)
                .build()
                .thenAccept(renderable -> welcomeViewRenderable = renderable);
        ModelRenderable.builder()
                .setSource(this, R.raw.model)
                .build()
                .thenAccept(renderable -> arrowRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Rendering Failed", throwable);
                            return null;
                        });
        ModelRenderable.builder()
                .setSource(this, R.raw.directionalarrow)
                .build()
                .thenAccept(renderable -> turnRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Rendering Failed", throwable);
                            return null;
                        });
    }

    @WorkerThread
    public boolean buildAugmentedImageDatabase(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;
        Bitmap[] augmentedImageBitmap = loadAugmentedImage();
        config.setFocusMode(Config.FocusMode.AUTO);
        if (augmentedImageBitmap == null) {
            return false;
        }
        augmentedImageDatabase = new AugmentedImageDatabase(session);
        augmentedImageDatabase.addImage("GO", augmentedImageBitmap[0]);
        augmentedImageDatabase.addImage("RIGHT", augmentedImageBitmap[1]);
        augmentedImageDatabase.addImage("WELCOME", augmentedImageBitmap[2]);
        augmentedImageDatabase.addImage("IN", augmentedImageBitmap[3]);
        augmentedImageDatabase.addImage("OUT", augmentedImageBitmap[4]);
        config.setAugmentedImageDatabase(augmentedImageDatabase);
        session.configure(config);
        return true;
    }

    @WorkerThread
    private Bitmap[] loadAugmentedImage() {
        Bitmap[] value = new Bitmap[5];
        try {
            InputStream inputStreamOne = getAssets().open("goCode.jpeg");
            value[0] = BitmapFactory.decodeStream(inputStreamOne);
        } catch (IOException e) {
            Log.d(TAG, "IO Exception loading Augmented Image One", e);
        }
        try {
            InputStream inputStreamTwo = getAssets().open("rightCode.jpeg");
            value[1] = BitmapFactory.decodeStream(inputStreamTwo);
        } catch (IOException e) {
            Log.d(TAG, "IO Exception loading Augmented Image Two", e);
        }
        try {
            InputStream inputStreamThree = getAssets().open("welcomeNote.jpg");
            value[2] = BitmapFactory.decodeStream(inputStreamThree);
        } catch (IOException e) {
            Log.d(TAG, "IO Exception loading Augmented Image Two", e);
        }
        try {
            InputStream inputStreamFour = getAssets().open("accessIn.jpg");
            value[3] = BitmapFactory.decodeStream(inputStreamFour);
        } catch (IOException e) {
            Log.d(TAG, "IO Exception loading Augmented Image Two", e);
        }
        try {
            InputStream inputStreamFive = getAssets().open("accessOut.jpg");
            value[4] = BitmapFactory.decodeStream(inputStreamFive);
        } catch (IOException e) {
            Log.d(TAG, "IO Exception loading Augmented Image Two", e);
        }
        return value;
    }

    public void onUpdateFrame(FrameTime frameTime) {
        session.getConfig().setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
        Frame frame = arSceneView.getArFrame();
        Collection<AugmentedImage> updateAugmentedImage = frame.getUpdatedTrackables
                (AugmentedImage.class);
        for (AugmentedImage image : updateAugmentedImage) {
            if (image.getTrackingState() == TrackingState.TRACKING) {
                if (image.getName().equals("GO")) {
                    AnchorNode arNodeOne = new AnchorNode();
                    if (arNodeOne.getRenderable() != arrowRenderable) {
                        arNodeOne.setAnchor(image.createAnchor(image.getCenterPose()));
                        arNodeOne.setRenderable(arrowRenderable);
                        arNodeOne.setName("GO");
                        arNodeOne.setParent(arSceneView.getScene());
                        if (countGo == 1) {
                            renderingPath(arNodeOne.getAnchor(), arNodeOne);
                        }
                    }
                }
                if (image.getName().equals("RIGHT")) {
                    AnchorNode arNodeTwo = new AnchorNode();
                    if (arNodeTwo.getRenderable() != turnRenderable) {
                        arNodeTwo.setAnchor(image.createAnchor(image.getCenterPose()));
                        arNodeTwo.setRenderable(turnRenderable);
                        arNodeTwo.setName("RIGHT");
                        arNodeTwo.setParent(arSceneView.getScene());
                        if (countRight == 1) {
                            renderingPath(arNodeTwo.getAnchor(), arNodeTwo);
                        }
                    }
                }
                if (image.getName().equals("WELCOME")) {
                    AnchorNode arNodeThree = new AnchorNode(image.createAnchor(image.getCenterPose()));
                    arNodeThree.setRenderable(welcomeViewRenderable);
                    arNodeThree.setParent(arSceneView.getScene());
                    View view = welcomeViewRenderable.getView();
                    ViewPager viewPager = view.findViewById(R.id.textWelcome);
                }
            }
        }
    }

    @WorkerThread
    private void renderingPath(Anchor anchor, AnchorNode anchorNode) {
        int pathGo = 0;
        int pathRight = 0;
        countGo = 0;
        session = arFragment.getArSceneView().getSession();
        configureSession();
        if (anchorNode.getName() == "GO") {
            Pose poseOne = anchor.getPose();
            float x = poseOne.tx();
            float y = poseOne.ty();
            float z = poseOne.tz() - .5f;
            while (pathGo < 2) {
                if (TrackingState.TRACKING == anchor.getTrackingState()) {
                    AnchorNode anchorNodeOne = new AnchorNode();
                    anchorNodeOne.setLocalPosition(new Vector3(x, y, z));
                    anchorNodeOne.setRenderable(turnRenderable);
                    anchorNodeOne.setParent(arSceneView.getScene());
                }
                ++pathGo;
                --z;
            }
        }
        if (anchorNode.getName() == "RIGHT") {
            countRight = 0;
            Pose poseTwo = anchor.getPose();
            float x = poseTwo.tx() + .5f;
            float y = poseTwo.ty();
            float z = poseTwo.tz();
            while (pathRight < 2) {
                if (TrackingState.TRACKING == anchor.getTrackingState()) {
                    AnchorNode anchorNodeTwo = new AnchorNode();
                    anchorNodeTwo.setLocalPosition(new Vector3(x, y, z));
                    anchorNodeTwo.setRenderable(turnRenderable);
                    anchorNodeTwo.setParent(arSceneView.getScene());
                }
                ++pathRight;
                ++x;
            }
        }
    }

    private void configureSession() {
        Config config = new Config(session);
        if (!buildAugmentedImageDatabase(config)) {
            Toast.makeText(this, "Unable to setup augmented", Toast.LENGTH_SHORT)
                    .show();
        }
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        session.configure(config);
        session.getConfig().setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            arSceneView.pause();
            session.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session == null) {
            String message = null;
            Exception exception = null;
            try {
                session = new Session(this);
            } catch (UnavailableArcoreNotInstalledException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update android";
                exception = e;
            } catch (Exception e) {
                message = "AR is not supported";
                exception = e;
            }

            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
            sessionConfigured = true;
        }
        if (sessionConfigured) {
            configureSession();
            sessionConfigured = false;
            arSceneView.setupSession(session);
        }
    }
}
