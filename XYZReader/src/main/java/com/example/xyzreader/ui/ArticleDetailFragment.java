package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.github.florent37.picassopalette.PicassoPalette;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A simple {@link Fragment} subclass.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleDetailFragment.class.getName();
    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private Toolbar toolbar;
    private FloatingActionButton fab;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView mPhotoView;
    private TextView tvAutor;
    private TextView tvContentInfo;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        toolbar = mRootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        collapsingToolbar = mRootView.findViewById(R.id.collapsing_toolbar);
        mPhotoView = mRootView.findViewById(R.id.backdrop);
        tvAutor = mRootView.findViewById(R.id.tvAutor);
        tvContentInfo = mRootView.findViewById(R.id.tvContentInfo);
        fab = mRootView.findViewById(R.id.share_fab);
        bindViews();
        return mRootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

//    public int getUpButtonFloor() {
//        if (mPhotoView == null || mPhotoView.getHeight() == 0) {
//            return Integer.MAX_VALUE;
//        }
//
//        // account for parallax
//        return mPhotoView.getHeight();
//    }

    private void bindViews() {
        Log.d(TAG, "Rootview " + mRootView + " cursor " + mCursor);
        if (mRootView == null) {
            return;
        }

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            bindImage();
            String title = bindTitle();
            String author = getAuthorFromCursor();

            collapsingToolbar.setTitle(title);

            tvAutor.setText(getString(R.string.format_title_detail, title, author));
            bindContent();
            setFabSharingAction(author, title);

        } else {
            mRootView.setVisibility(View.GONE);
            tvAutor.setText("N/A");
            tvAutor.setText("N/A");
            tvContentInfo.setText("N/A");
        }
    }

    private void bindImage() {
        String imgUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
        Picasso.get()
                .load(imgUrl)
                .fit()
                .centerCrop()
                .into(mPhotoView,
                        PicassoPalette.with(imgUrl, mPhotoView)
                                .use(PicassoPalette.Profile.VIBRANT)
                                .intoBackground(tvAutor)
                                .intoTextColor(tvAutor, PicassoPalette.Swatch.TITLE_TEXT_COLOR));
    }

    private String bindTitle() {
        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        if (title == null) {
            title = "";
        }

        return title;
    }

    private String getAuthorFromCursor() {
        String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
        if (author == null) {
            author = "";
        }
        return author;
    }

    private void bindContent() {
        Date date = new Date();
        String content = mCursor.getString(ArticleLoader.Query.BODY);
        Log.d(TAG, "Tiempo en traer contenido " + (new Date().getTime() - date.getTime()));

        Date date2 = new Date();
        tvContentInfo.setText(content);
        Log.d(TAG, "Tiempo en bindear contenido " + (new Date().getTime() - date2.getTime()));
    }

    private void setFabSharingAction(final String autor, final String title) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getString(R.string.format_sharing_text, autor, title))
                        .getIntent(), getString(R.string.action_share)));
            }
        });
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }
}
