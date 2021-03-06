package me.sankalpchauhan.positively.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.sankalpchauhan.positively.R;
import me.sankalpchauhan.positively.config.Constants;
import me.sankalpchauhan.positively.database.AppDatabase;
import me.sankalpchauhan.positively.service.model.Quotes;
import me.sankalpchauhan.positively.service.repository.QuotesRepository;
import me.sankalpchauhan.positively.utils.AppExecutors;
import timber.log.Timber;

public class QuoteDetailActivity extends AppCompatActivity {
    @BindView(R.id.adView)
    AdView adView;
    @BindView(R.id.share_fab)
    FloatingActionButton shareFAB;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.quote_image)
    ImageView quoteImage;
    @BindView(R.id.quote_actual)
    TextView quoteText;
    @BindView(R.id.quote_authour)
    TextView quoteAuthour;
    Quotes quotes;
    String imageUrl;
    private Menu menu;
    private MutableLiveData<Boolean> isFav = new MutableLiveData<>();
    private AppDatabase roomDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent i = getIntent();
        if (i.hasExtra(Constants.QUOTES_DATA)) {
            quotes = (Quotes) i.getSerializableExtra(Constants.QUOTES_DATA);
            imageUrl = i.getStringExtra(Constants.QUOTES_IMAGE_DATA);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quote_detail);
        ButterKnife.bind(this);
        roomDB = AppDatabase.getInstance(getApplicationContext());
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitle(getResources().getString(R.string.positive_ly_quote_title));
        isFav.setValue(false);
        invalidateOptionsMenu();
        Picasso.get().load(imageUrl).into(quoteImage);
        quoteText.setText(quotes.getText());
        if (quotes.getAuthor() != null) {
            quoteAuthour.setText(String.format("- By %s", quotes.getAuthor()));
        } else {
            quoteAuthour.setVisibility(View.GONE);
        }
        shareFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String shareBody = "\" " + quotes.getText() + "\" " + getResources().getString(R.string.get_download_now);
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject));
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
            }
        });
        isQuoteFavorite();

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.fav_icon:
                saveDataToRoom();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveDataToRoom() {
        final Quotes q = new Quotes(quotes.getText(), quotes.getAuthor(), imageUrl);
        if (!isFav.getValue()) {
            Timber.e("I am here1");
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    roomDB.quotesDao().InsertQuoteToRoom(q);
                }
            });
        } else {
            Timber.e("I am here2");
            isFav.setValue(false);
            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    roomDB.quotesDao().deleteQuoteById(quotes.getText(), imageUrl);
                }
            });
        }


    }

    private void isQuoteFavorite() {
        roomDB.quotesDao().getCurrentQuoteById(quotes.getText(), imageUrl).observe(QuoteDetailActivity.this, new Observer<Quotes>() {
            @Override
            public void onChanged(Quotes q) {
                if (q != null) {
                    if (q.getText().equals(quotes.getText())) {
                        isFav.setValue(true);
                        invalidateOptionsMenu();
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.quotes_detail_menu, menu);
        isFav.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                menu.getItem(0).setIcon(ContextCompat.getDrawable(QuoteDetailActivity.this, R.drawable.ic_star_border_black_24dp));
                if (aBoolean.equals(true)) {
                    menu.getItem(0).setIcon(ContextCompat.getDrawable(QuoteDetailActivity.this, R.drawable.ic_star_black_24dp));
                }
            }
        });
        return true;
    }
}
