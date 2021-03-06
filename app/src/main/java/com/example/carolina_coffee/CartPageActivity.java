package com.example.carolina_coffee;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.text.DecimalFormat;

public class CartPageActivity extends AppCompatActivity {
    //int score=0;/

    FirebaseAuth fAuth;
    FirebaseUser user;
    FirebaseFirestore fStore;
    String userID;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;


    //private static final String SCORE = "Score";
    RecyclerView recyler_menu;
    RecyclerView.LayoutManager layoutManager;

    RecyclerView cart_addin_recyler_menu;
    RecyclerView.LayoutManager cart_addin_layoutManager;

    RecyclerView.Adapter<CartViewHolder> adapter;
    RecyclerView.Adapter<CartAddinViewHolder> cart_addin_adapter;

    TextView cart_price;

    Cart cart = OrderMenuPageActivity.getCart();
    Latte drink;

    //Dialog box for payment method
    AlertDialog dialog;
    AlertDialog.Builder builder;
    String[] both_payments = {"Payment Method 1", "Payment Method 2"};
    String[] payment1_only = {"Payment Method 1"};
    String[] payment2_only = {"Payment Method 2"};
    String[] items_w_rewards = {"Redeem $5 Coupon", "Do Not Redeem"};
    String result = "";
    String result2 = "";

    // Keep record if payment exists here.
    String check_payment_1 = "no"; // default
    String check_payment_2 = "no"; // default

    // Rewards Increment - save data
    public static final String SHARED_PREF = "Myscore";
    public static final String SHARED_PREFS = "sharedPrefs";
    int newRewardsNum= 0;

    NumberFormat f;


    private static final String TAG = "TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //------------------------
        // Comes first to get rid of the white default loading screen
        // Eseentially this is wokring to bypass and remove default white screen that pops up when loading app.
        // May change this later. ALSO This is also linking to Themes.xml's under Res/Values/themes.
        setTheme(R.style.Theme_Carolina_Coffee);
        //------------------------

        // Get rid of the top "Carolina_Coffee" purple bar on top of each page.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        getSupportActionBar().hide(); // This line will hide the action bar

        // This will change the action bar color from the default purple, to color of choice here.
        // Calling to method that will make this action happen.
        statusBarColor();
        setContentView(R.layout.activity_cart_page);

        updatePaymentMethods();



        // Navigation
        //--------------------------------------------------------------------------------------
        //Initialize and assign variables - bottom_navigation = nav bar inside activity_main.xml
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        //Set home Selected
        bottomNavigationView.setSelectedItemId(R.id.payPageButton);
        //perform itemSelectedListener
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    //Home Page Button
                    case R.id.homePageButton:
                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        overridePendingTransition(0,0);
                        return true;

                    //Payment Page Button
                    case R.id.payPageButton:
                        return true;

                    //Order Page Button
                    case R.id.orderPageButton:
                        startActivity(new Intent(getApplicationContext(), MenuPageActivity.class));
                        overridePendingTransition(0,0);
                        return true;

                    //Account Page Button
                    case R.id.accountPageButton:
                        startActivity(new Intent(getApplicationContext(), SettingPageActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                }
                return false;
            }
        });
        // End of Navigation
        //--------------------------------------------------------------------------------------



        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        newRewardsNum = sharedPreferences.getInt(SHARED_PREF + userID, 0);

        //End of onCreate
        //--------------------------------------------------------------------------------------------------------
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        userID = fAuth.getCurrentUser().getUid();
        user = fAuth.getCurrentUser();

        firebaseDatabase = FirebaseDatabase.getInstance();

        //Load menu
        recyler_menu = findViewById(R.id.reviewOrderRecycler);
        layoutManager = new LinearLayoutManager(this);
        recyler_menu.setLayoutManager(layoutManager);
        recyler_menu.setHasFixedSize(false);

        //Brianna- can be used for formatting currency
        f = NumberFormat.getInstance();
        if (f instanceof DecimalFormat) {
            ((DecimalFormat) f).setDecimalSeparatorAlwaysShown(true);
            ((DecimalFormat) f).setMinimumFractionDigits(2);
        }

        loadCart();
        cart_price = findViewById(R.id.cart_cost_text1);
        cart.calaculateCostofCart();
        cart_price.setText("$" + f.format(cart.total_cart_price));

        /*
        //for rewards
        setContentView(R.layout.activity_cart_page);
        SharedPreferences sp=this.getSharedPreferences("MyScore", Context.MODE_PRIVATE);
        score=sp.getInt("score", 0);

        //Button finalPlaceOrderButtonRewards = findViewById(R.id.finalplaceOrderButton);
        //finalPlaceOrderButtonRewards.setOnClickListener(this);

        rewardsNum = (TextView) findViewById(R.id.earned_points);
        increase = (Button) findViewById(R.id.finalplaceOrderButton);
        increase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newRewardsNum++;
                rewardsNum.setText(String.valueOf(newRewardsNum));
            }
        });

         */


    }

    public void updatePaymentMethods() {
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        userID = fAuth.getCurrentUser().getUid();
        user = fAuth.getCurrentUser();

        firebaseDatabase = FirebaseDatabase.getInstance();
        // Check
        // Determine if user has payment 1 method activated.
        //----------------------------------------------------------------------------------------------------------------------
        DocumentReference documentReference1 = fStore.collection("PaymentMethod_1").document(userID);
        documentReference1.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            // Card 1
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot1, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot1.exists()) {

                    if(documentSnapshot1.getString("Billing_Card_Num_1") == null) {
                        // no card
                        String check_payment_1 = "";
                        payment_1_no();

                    } else {
                        // Card found.
                        payment_1_yes();

                    }
                } else {
                    Log.d("tag", "onEvent: Document do not exists");
                }
            }
        });

        // Determine if user has payment 2 method activated.
        DocumentReference documentReference2 = fStore.collection("PaymentMethod_2").document(userID);
        documentReference2.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            // Card 2
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot2, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot2.exists()) {

                    if(documentSnapshot2.getString("Billing_Card_Num_2") == null) {
                        // no card
                        String check_payment_2 = "";
                        payment_2_no();

                    } else {
                        String check_payment_2 = "yes";
                        payment_2_yes();
                    }
                } else {
                    Log.d("tag", "onEvent: Document do not exists");
                }
            }
        });

        //----------------------------------------------------------------------------------------------------------------------
    }

    public void backButton(View view) {
        Intent intent = new Intent(this, MenuPageActivity.class);
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    private void loadCart() {
        adapter = new RecyclerView.Adapter<CartViewHolder>() {
            @Override
            public void onBindViewHolder(CartViewHolder viewHolder, int i) {
                drink = cart.getCart().get(i);
                viewHolder.txtDrinkName.setText(drink.getFullName());

                cart_addin_recyler_menu = viewHolder.addinRecycler;
                cart_addin_layoutManager = new LinearLayoutManager(CartPageActivity.this);
                cart_addin_recyler_menu.setLayoutManager(cart_addin_layoutManager);
                cart_addin_recyler_menu.setHasFixedSize(false);
                loadCartAddins();

                viewHolder.txtDrinkPrice.setText("$" + f.format(drink.getPrice()));

                viewHolder.removeButton.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                cart.removefromCart(cart.getCart().get(viewHolder.getAdapterPosition()));
                                Intent intent = new Intent(CartPageActivity.this, CartPageActivity.class);
                                overridePendingTransition(0, 0);
                                startActivity(intent);
                            }
                        }
                );
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        Toast.makeText(CartPageActivity.this, "Drink was clicked!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public int getItemCount() {
                return cart.getCart().size();
            }

            @NonNull
            @Override
            public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.review_order_item, parent, false);
                return new CartViewHolder(view);
            }
        };
        recyler_menu.setAdapter(adapter);
    }

    public void loadCartAddins() {
        cart_addin_adapter = new RecyclerView.Adapter<CartAddinViewHolder>() {
            @Override
            public void onBindViewHolder(CartAddinViewHolder viewHolder, int i) {
                viewHolder.cart_addin_txt.setText(drink.getAdditions().get(i));

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        Toast.makeText(CartPageActivity.this, "Add-in was clicked!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return drink.getAdditions().size();
            }

            @NonNull
            @Override
            public CartAddinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cart_addin, parent, false);
                return new CartAddinViewHolder(view);
            }
        };
        cart_addin_recyler_menu.setAdapter(cart_addin_adapter);
    }


    // This is method to change the status bar color from default purple to color of choice.
    private void statusBarColor() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black,this.getTheme()));
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
    }

    public void paymentPageButton(View view) {
        if(cart.getCart().size() == 0) {
            Toast.makeText(CartPageActivity.this, "Cart is empty!", Toast.LENGTH_SHORT).show();
        } else {
            // Creates a dialog box to ask user which payment method to use.
            // If a payment method does not exists, it will not let used buy.
            // -----------------------------------------------------
            // Needs to ask user which payment method and only accept the payment method if it is valid,
            // and then keeps track of that data of last 4 card digits to order.




            // User does not have rewards points to spend.
            //else {


                // Determine if user has payment 1 method activated.
                DocumentReference documentReference1 = fStore.collection("PaymentMethod_1").document(userID);
                documentReference1.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
                    // Card 1
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot1, @Nullable FirebaseFirestoreException e) {
                        if (documentSnapshot1.exists()) {
                            String card_1 = documentSnapshot1.getString("Billing_Card_Num_1");
                            String lastFourDigits_1 = "";
                            if(documentSnapshot1.getString("Billing_Card_Num_1") == null) {
                                // no card
                                String check_payment_1 = "";
                                payment_1_no();

                            } else {
                                // Card found.
                                payment_1_yes();

                            }
                        } else {
                            Log.d("tag", "onEvent: Document do not exists");
                        }
                    }
                });

                // Determine if user has payment 2 method activated.
                DocumentReference documentReference2 = fStore.collection("PaymentMethod_2").document(userID);
                documentReference2.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
                    // Card 2
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot2, @Nullable FirebaseFirestoreException e) {
                        if (documentSnapshot2.exists()) {
                            String card_1 = documentSnapshot2.getString("Billing_Card_Num_2");
                            String lastFourDigits_1 = "";
                            if(documentSnapshot2.getString("Billing_Card_Num_2") == null) {
                                // no card
                                String check_payment_2 = "";
                                payment_2_no();

                            } else {
                                String check_payment_2 = "yes";
                                payment_2_yes();
                            }
                        } else {
                            Log.d("tag", "onEvent: Document do not exists");
                        }
                    }
                });


                //Once yes/no values set for payment, run this to run dialog prompt boxes for payment.
                check_Payment_1_2_yes_no(view);









/*
                //--------------------------------------------------------
                // Creating pop-up dialog box..
                builder = new AlertDialog.Builder(CartPageActivity.this);
                builder.setTitle("Select Payment Method");
                builder.setSingleChoiceItems(both_payments, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result = both_payments[which];
                    }
                });

                // YES button - User clicked YES
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User selects which payment method to use.
                        if (result == "Payment Method 1") {
                            //  Check that card is existing and use.
                            checkPayment_1_Exists(view);
                        } else if (result == "Payment Method 2") {
                            //  Check that card is existing and use.
                            checkPayment_2_Exists(view);
                        } else {
                            //TODO
                            // this error plays when i select payment 1, not sure why.
                            Toast.makeText(getApplicationContext(), "You did not select a payment", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                // CANCEL button - user clicked CANCEL
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Will automatically exit dialog box.
                        // This will fix a bug that when you click and then unclick "confirm" bubble, it still lets you delete account even if you did not click confirm bubble on second attempt.
                        // Issue resolved.
                        result = "";
                    }
                });
                dialog = builder.create();
                dialog.show();
                // -----------------------------------------------------

 */
            }

        }


    //}


    public void payment_1_yes(){
        check_payment_1 = "yes";
        return;
    }
    public void payment_1_no(){
        check_payment_1 = "no";
        return;
    }
    public void payment_2_yes(){
        check_payment_2 = "yes";
        return;
    }
    public void payment_2_no(){
        check_payment_2 = "no";
        return;
    }


    public void check_Payment_1_2_yes_no(View view) {
        // Both payments exists.
        if(check_payment_1 == "yes" && check_payment_2 == "yes") {
            //Toast.makeText(getApplicationContext(), "both", Toast.LENGTH_LONG).show();
            fAuth = FirebaseAuth.getInstance();
            fStore = FirebaseFirestore.getInstance();
            userID = fAuth.getCurrentUser().getUid();
            user = fAuth.getCurrentUser();

            //Confirm Payment Dialog
            // If user has rewards
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, 0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            newRewardsNum = sharedPreferences.getInt(SHARED_PREF + userID, 0);
            //TODO
            // User has >4 = rewards points
            // ////
            // ----------
            if(newRewardsNum >= 4 && cart.total_cart_price >= 6) {
                // Creating pop-up dialog box..
                builder = new AlertDialog.Builder(CartPageActivity.this);
                builder.setTitle("Select Payment Method");
                builder.setSingleChoiceItems(both_payments, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result = both_payments[which];
                    }
                });

                // YES button - User clicked YES
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User selects which payment method to use.
                        if (result == "Payment Method 1") {
                            //  Check that card is existing and use.
                            //checkPayment_1_Exists_Rewards(view);

                            // Asks user if they want to redeem points
                            redeem_rewards_box_p1(view);
                        } else if (result == "Payment Method 2") {
                            //  Check that card is existing and use.
                            //checkPayment_2_Exists_Rewards(view);

                            // Asks user if they want to redeem points
                            redeem_rewards_box_p2(view);
                        } else {
                            //TODO
                            // this error plays when i select payment 1, not sure why.
                            Toast.makeText(getApplicationContext(), "You did not select a payment", Toast.LENGTH_LONG).show();
                        }

                    }
                });

                // CANCEL button - user clicked CANCEL
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Will automatically exit dialog box.
                        // This will fix a bug that when you click and then unclick "confirm" bubble, it still lets you delete account even if you did not click confirm bubble on second attempt.
                        // Issue resolved.
                        result = "";
                    }
                });
                dialog = builder.create();
                dialog.show();
            } else {
                //--------------------------------------------------------
                // Creating pop-up dialog box..
                builder = new AlertDialog.Builder(CartPageActivity.this);
                builder.setTitle("Select Payment Method");
                builder.setSingleChoiceItems(both_payments, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result = both_payments[which];
                    }
                });

                // YES button - User clicked YES
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User selects which payment method to use.
                        if (result == "Payment Method 1") {
                            //  Check that card is existing and use.
                            checkPayment_1_Exists(view);
                        } else if (result == "Payment Method 2") {
                            //  Check that card is existing and use.
                            checkPayment_2_Exists(view);
                        } else {
                            //TODO
                            // this error plays when i select payment 1, not sure why.
                            Toast.makeText(getApplicationContext(), "You did not select a payment", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                // CANCEL button - user clicked CANCEL
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Will automatically exit dialog box.
                        // This will fix a bug that when you click and then unclick "confirm" bubble, it still lets you delete account even if you did not click confirm bubble on second attempt.
                        // Issue resolved.
                        result = "";
                    }
                });
                dialog = builder.create();
                dialog.show();
                // -----------------------------------------------------
            }
        }

        // ONLY payment 1 exists.
        else if(check_payment_1 == "yes" && check_payment_2 == "no") {
            //Toast.makeText(getApplicationContext(), "only 1", Toast.LENGTH_LONG).show();
            fAuth = FirebaseAuth.getInstance();
            fStore = FirebaseFirestore.getInstance();
            userID = fAuth.getCurrentUser().getUid();
            user = fAuth.getCurrentUser();

            //Confirm Payment Dialog
            // If user has rewards
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, 0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            newRewardsNum = sharedPreferences.getInt(SHARED_PREF + userID, 0);
            //TODO
            // User has >4 = rewards points
            // ////
            // ----------
            if(newRewardsNum >= 4 && cart.total_cart_price >= 6) {
                // Creating pop-up dialog box..
                builder = new AlertDialog.Builder(CartPageActivity.this);
                builder.setTitle("Select Payment Method");
                builder.setSingleChoiceItems(payment1_only, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result = payment1_only[which];
                    }
                });

                // YES button - User clicked YES
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User selects which payment method to use.
                        if (result == "Payment Method 1") {
                            //  Check that card is existing and use.
                            //checkPayment_1_Exists_Rewards(view);

                            // Asks user if they want to redeem points
                            redeem_rewards_box_p1(view);

                        } else {
                            //TODO
                            // this error plays when i select payment 1, not sure why.
                            Toast.makeText(getApplicationContext(), "You did not select a payment", Toast.LENGTH_LONG).show();
                        }

                    }
                });

                // CANCEL button - user clicked CANCEL
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Will automatically exit dialog box.
                        // This will fix a bug that when you click and then unclick "confirm" bubble, it still lets you delete account even if you did not click confirm bubble on second attempt.
                        // Issue resolved.
                        result = "";
                    }
                });
                dialog = builder.create();
                dialog.show();
            } else {
                //--------------------------------------------------------
                // Creating pop-up dialog box..
                builder = new AlertDialog.Builder(CartPageActivity.this);
                builder.setTitle("Select Payment Method");
                builder.setSingleChoiceItems(payment1_only, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result = payment1_only[which];
                    }
                });

                // YES button - User clicked YES
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User selects which payment method to use.
                        if (result == "Payment Method 1") {
                            //  Check that card is existing and use.
                            checkPayment_1_Exists(view);

                        } else {
                            //TODO
                            // this error plays when i select payment 1, not sure why.
                            Toast.makeText(getApplicationContext(), "You did not select a payment", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                // CANCEL button - user clicked CANCEL
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Will automatically exit dialog box.
                        // This will fix a bug that when you click and then unclick "confirm" bubble, it still lets you delete account even if you did not click confirm bubble on second attempt.
                        // Issue resolved.
                        result = "";
                    }
                });
                dialog = builder.create();
                dialog.show();
                // -----------------------------------------------------
            }
        }

        // ONLY payment 2 exists.
        else if(check_payment_1 == "no" && check_payment_2 == "yes") {
            //Toast.makeText(getApplicationContext(), "only 2", Toast.LENGTH_LONG).show();
            fAuth = FirebaseAuth.getInstance();
            fStore = FirebaseFirestore.getInstance();
            userID = fAuth.getCurrentUser().getUid();
            user = fAuth.getCurrentUser();

            //Confirm Payment Dialog
            // If user has rewards
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, 0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            newRewardsNum = sharedPreferences.getInt(SHARED_PREF + userID, 0);
            //TODO
            // User has >4 = rewards points
            // ////
            // ----------
            if(newRewardsNum >= 4 && cart.total_cart_price >= 6) {
                // Creating pop-up dialog box..
                builder = new AlertDialog.Builder(CartPageActivity.this);
                builder.setTitle("Select Payment Method");
                builder.setSingleChoiceItems(payment2_only, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result = payment2_only[which];
                    }
                });

                // YES button - User clicked YES
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User selects which payment method to use.
                        if (result == "Payment Method 2") {
                            //  Check that card is existing and use.
                            //checkPayment_1_Exists_Rewards(view);

                            // Asks user if they want to redeem points
                            redeem_rewards_box_p1(view);

                        } else {
                            //TODO
                            // this error plays when i select payment 1, not sure why.
                            Toast.makeText(getApplicationContext(), "You did not select a payment", Toast.LENGTH_LONG).show();
                        }

                    }
                });

                // CANCEL button - user clicked CANCEL
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Will automatically exit dialog box.
                        // This will fix a bug that when you click and then unclick "confirm" bubble, it still lets you delete account even if you did not click confirm bubble on second attempt.
                        // Issue resolved.
                        result = "";
                    }
                });
                dialog = builder.create();
                dialog.show();
            } else {
                //--------------------------------------------------------
                // Creating pop-up dialog box..
                builder = new AlertDialog.Builder(CartPageActivity.this);
                builder.setTitle("Select Payment Method");
                builder.setSingleChoiceItems(payment2_only, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result = payment2_only[which];
                    }
                });

                // YES button - User clicked YES
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User selects which payment method to use.
                        if (result == "Payment Method 2") {
                            //  Check that card is existing and use.
                            checkPayment_1_Exists(view);

                        } else {
                            //TODO
                            // this error plays when i select payment 1, not sure why.
                            Toast.makeText(getApplicationContext(), "You did not select a payment", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                // CANCEL button - user clicked CANCEL
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Will automatically exit dialog box.
                        // This will fix a bug that when you click and then unclick "confirm" bubble, it still lets you delete account even if you did not click confirm bubble on second attempt.
                        // Issue resolved.
                        result = "";
                    }
                });
                dialog = builder.create();
                dialog.show();
                // -----------------------------------------------------
            }
        }

        // NO payments exists.
        else if(check_payment_1 == "no" && check_payment_2 == "no"){
            Toast.makeText(getApplicationContext(), "You do not have any payment methods.", Toast.LENGTH_LONG).show();
        }

        // Bad No
        else {
            // NO payments exists.
            Toast.makeText(getApplicationContext(), "Updating, Try again.", Toast.LENGTH_LONG).show();
            //set_payment_1_2_yes_no();
        }
    }




    public void checkPayment_1_Exists(View view) {
        DocumentReference documentReference = fStore.collection("PaymentMethod_1").document(userID);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot.exists()){
                    // Checks if users payment card exists.
                    Map<String, Object> map = documentSnapshot.getData();
                    if (map.size() == 0) {
                        Toast.makeText(getApplicationContext(), "Payment method does not exist.", Toast.LENGTH_LONG).show();
                    } else {
                        // Add increment to shared Pref.
                        addIncSharedPref();

                        FirebaseUser fuser = fAuth.getCurrentUser();
                        userID = fAuth.getCurrentUser().getUid();

                        // increment value in firestore
                       // addIncFirebaseStore();




                        // Establish card used:
                        String card_1 = documentSnapshot.getString("Billing_Card_Num_1");

                        // This reads the card successfully
                        //Toast.makeText(getApplicationContext(), card_2, Toast.LENGTH_LONG).show();
                        addDataToFireBase(card_1);
                    }
                }else {
                    Toast.makeText(getApplicationContext(), "Payment method does not exist.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    public void checkPayment_2_Exists(View view) {
        DocumentReference documentReference = fStore.collection("PaymentMethod_2").document(userID);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot.exists()){
                    // Checks if users payment card exists.
                    Map<String, Object> map = documentSnapshot.getData();
                    if (map.size() == 0) {
                        //Log.d(TAG, "Document is empty!");
                        Toast.makeText(getApplicationContext(), "Payment method does not exist.", Toast.LENGTH_LONG).show();
                    } else {
                        // Add increment to shared Pref.
                        addIncSharedPref();

                        FirebaseUser fuser = fAuth.getCurrentUser();
                        userID = fAuth.getCurrentUser().getUid();

                        // Establish card used:
                        String card_2 = documentSnapshot.getString("Billing_Card_Num_2");

                        // This reads the card successfully
                        //Toast.makeText(getApplicationContext(), card_2, Toast.LENGTH_LONG).show();
                        addDataToFireBase(card_2);

                    }
                }else {
                    Toast.makeText(getApplicationContext(), "Payment method does not exist.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    // Dialog box to use rewards or not Payment 1
    public void redeem_rewards_box_p1(View view) {
        // Creating pop-up dialog box..
        builder = new AlertDialog.Builder(CartPageActivity.this);
        builder.setTitle("You have a $5 Coupon!");
        builder.setSingleChoiceItems(items_w_rewards, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                result2 = items_w_rewards[which];
            }
        });

        // YES button - User clicked YES
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User selects which payment method to use.
                if (result2 == "Redeem $5 Coupon") {
                    //  Check that card is existing and use. + rewards
                    checkPayment_1_Exists_Rewards(view);
                } else if (result2 == "Do Not Redeem") {
                    //  Check that card is existing and use.
                    checkPayment_1_Exists(view);
                } else {
                    //TODO
                    // this error plays when i select payment 1, not sure why.
                    Toast.makeText(getApplicationContext(), "You did not select a payment", Toast.LENGTH_LONG).show();
                }
            }
        });

        // CANCEL button - user clicked CANCEL
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Will automatically exit dialog box.
                // This will fix a bug that when you click and then unclick "confirm" bubble, it still lets you delete account even if you did not click confirm bubble on second attempt.
                // Issue resolved.
                result = "";
            }
        });
        dialog = builder.create();
        dialog.show();
        // -----------------------------------------------------
    }
    // Dialog box to use rewards or not Payment 2
    public void redeem_rewards_box_p2(View view) {
        // Creating pop-up dialog box..
        builder = new AlertDialog.Builder(CartPageActivity.this);
        builder.setTitle("You have a $5 Coupon!");
        builder.setSingleChoiceItems(items_w_rewards, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                result2 = items_w_rewards[which];
            }
        });

        // YES button - User clicked YES
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User selects which payment method to use.
                if (result2 == "Redeem $5 Coupon") {
                    //  Check that card is existing and use. + rewards
                    checkPayment_2_Exists_Rewards(view);
                } else if (result2 == "Do Not Redeem") {
                    //  Check that card is existing and use.
                    checkPayment_2_Exists(view);
                } else {
                    //TODO
                    // this error plays when i select payment 1, not sure why.
                    Toast.makeText(getApplicationContext(), "You did not select an option.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // CANCEL button - user clicked CANCEL
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Will automatically exit dialog box.
                // This will fix a bug that when you click and then unclick "confirm" bubble, it still lets you delete account even if you did not click confirm bubble on second attempt.
                // Issue resolved.
                result = "";
            }
        });
        dialog = builder.create();
        dialog.show();
        // ------------------------------------------------------
    }

    // For user redeeming rewards
    public void checkPayment_1_Exists_Rewards(View view) {
        DocumentReference documentReference = fStore.collection("PaymentMethod_1").document(userID);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot.exists()){
                    // Checks if users payment card exists.
                    Map<String, Object> map = documentSnapshot.getData();
                    if (map.size() == 0) {
                        Toast.makeText(getApplicationContext(), "Payment method does not exist.", Toast.LENGTH_LONG).show();
                    } else {

                        //decrement
                        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, 0);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        newRewardsNum = sharedPreferences.getInt(SHARED_PREF+ userID, 0) -4;//-4
                        editor.putInt(SHARED_PREF + userID, newRewardsNum);
                        editor.apply();

                        FirebaseUser fuser = fAuth.getCurrentUser();
                        userID = fAuth.getCurrentUser().getUid();

                        // Establish card used:
                        String card_1 = documentSnapshot.getString("Billing_Card_Num_1");

                        // This reads the card successfully
                        //Toast.makeText(getApplicationContext(), card_2, Toast.LENGTH_LONG).show();
                        addDataToFireBase_Rewards(card_1);
                    }
                }else {
                    Toast.makeText(getApplicationContext(), "Payment method does not exist.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // For user redeeming rewards
    public void checkPayment_2_Exists_Rewards(View view) {
        DocumentReference documentReference = fStore.collection("PaymentMethod_2").document(userID);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot.exists()){
                    // Checks if users payment card exists.
                    Map<String, Object> map = documentSnapshot.getData();
                    if (map.size() == 0) {
                        //Log.d(TAG, "Document is empty!");
                        Toast.makeText(getApplicationContext(), "Payment method does not exist.", Toast.LENGTH_LONG).show();
                    } else {

                        //decrement
                        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, 0);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        newRewardsNum = sharedPreferences.getInt(SHARED_PREF + userID, 0) -4;
                        editor.putInt(SHARED_PREF + userID, newRewardsNum);
                        editor.apply();

                        FirebaseUser fuser = fAuth.getCurrentUser();
                        userID = fAuth.getCurrentUser().getUid();

                        // Establish card used:
                        String card_2 = documentSnapshot.getString("Billing_Card_Num_2");

                        // This reads the card successfully
                        //Toast.makeText(getApplicationContext(), card_2, Toast.LENGTH_LONG).show();
                        addDataToFireBase_Rewards(card_2);

                    }
                }else {
                    Toast.makeText(getApplicationContext(), "Payment method does not exist.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void addIncSharedPref() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        newRewardsNum = sharedPreferences.getInt(SHARED_PREF+ userID, 0) +1 ;
        editor.putInt(SHARED_PREF+ userID, newRewardsNum);
        editor.apply();
    }

    private void addIncFirebaseStore() {
        userID = fAuth.getCurrentUser().getUid();

        Log.d(TAG, "onSuccess: rewards_increment  " +  userID + " num = " + newRewardsNum);

        DocumentReference documentReference = fStore.collection("rewards_increment").document(userID);
        Map<String,Object> user = new HashMap<>();
        user.put("Increment", newRewardsNum );
        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: Incremented ");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: " + e);
            }
        });
    }

    private void addDataToFireBase(String s) {
        userID = fAuth.getCurrentUser().getUid();
        DocumentReference documentReference = fStore.collection("NewOrders").document(userID);
        Map<String, Object> map = new HashMap<>();

        documentReference.get().addOnCompleteListener(this, new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.getResult().exists()) {
                    Map<String, Object> mapPrev = task.getResult().getData();
                    for(Map.Entry<String, Object> entry : mapPrev.entrySet()) {
                        map.put(entry.getKey(), entry.getValue());
                    }
                }

                userID = fAuth.getCurrentUser().getUid();
                Order newOrder = new Order(userID, cart.getCart(), Double.parseDouble(f.format(cart.total_cart_price)), s);
                map.put("Order " + new Date().getTime(), newOrder);

                documentReference.set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: user Profile is created for "+ userID);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e);
                    }
                });

                // After a successful order submission, clear cart and send user to home page.
                cart.getCart().clear();
                Intent intent = new Intent(CartPageActivity.this, MainActivity.class);
                overridePendingTransition(0, 0);
                startActivity(intent);
                Toast.makeText(CartPageActivity.this, "Order was placed!", Toast.LENGTH_SHORT).show();
                Toast.makeText(CartPageActivity.this, "You've earned a reward point!", Toast.LENGTH_SHORT).show();
            }
        });



        /*
        // Transfer data to firebase.
        userID = fAuth.getCurrentUser().getUid();
        DocumentReference documentReference = fStore.collection("Orders").document(userID);
        Map<String,Object> user = new HashMap<>();
        user.put("Order_User_Name", userID);
        user.put("Drinks", cart.getCart());
        user.put("Order_Price", cart.total_cart_price);
        // Automatically knows which card the user submitted for payment.
        user.put("Payment_Used", s);
        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: user Profile is created for "+ userID);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: " + e);
            }
        });
*/

    }

    // For user redeeming rewards
    private void addDataToFireBase_Rewards(String s) {
        // Transfer data to firebase.
        userID = fAuth.getCurrentUser().getUid();
        DocumentReference documentReference = fStore.collection("NewOrders").document(userID);
        Map<String, Object> map = new HashMap<>();

        documentReference.get().addOnCompleteListener(this, new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.getResult().exists()) {
                    Map<String, Object> mapPrev = task.getResult().getData();
                    for(Map.Entry<String, Object> entry : mapPrev.entrySet()) {
                        map.put(entry.getKey(), entry.getValue());
                    }
                }

                userID = fAuth.getCurrentUser().getUid();
                Order newOrder = new Order(userID, cart.getCart(), Double.parseDouble(f.format(cart.total_cart_price-4.99)), s);
                map.put("Order " + new Date().getTime(), newOrder);

                documentReference.set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: user Profile is created for "+ userID);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e);
                    }
                });

                // After a successful order submission, clear cart and send user to home page.
                cart.getCart().clear();
                Intent intent = new Intent(CartPageActivity.this, MainActivity.class);
                overridePendingTransition(0, 0);
                startActivity(intent);
                Toast.makeText(CartPageActivity.this, "Order was placed!", Toast.LENGTH_SHORT).show();
                Toast.makeText(CartPageActivity.this, "You've redeemed your reward!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean order_More(View view) {
        startActivity(new Intent(getApplicationContext(), MenuPageActivity.class));
        overridePendingTransition(0,0);
        return true;
    }

}