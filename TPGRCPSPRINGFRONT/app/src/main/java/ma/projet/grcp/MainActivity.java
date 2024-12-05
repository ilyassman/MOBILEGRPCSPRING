package ma.projet.grcp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ma.projet.grcp.adapter.AccountAdapter;
import ma.projet.grpc.stubs.Compte;
import ma.projet.grpc.stubs.CompteRequest;
import ma.projet.grpc.stubs.CompteServiceGrpc;
import ma.projet.grpc.stubs.GetAllComptesRequest;
import ma.projet.grpc.stubs.GetAllComptesResponse;
import ma.projet.grpc.stubs.GetTotalSoldeRequest;
import ma.projet.grpc.stubs.GetTotalSoldeResponse;
import ma.projet.grpc.stubs.SaveCompteRequest;
import ma.projet.grpc.stubs.SaveCompteResponse;
import ma.projet.grpc.stubs.SoldeStats;
import ma.projet.grpc.stubs.TypeCompte;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerViewAccounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        btn=findViewById(R.id.addbtn);
        btn.setOnClickListener(v -> {
            showAddAccountDialog();
        });


        // existing code...
        new Thread(() -> {
            List<Compte> comptes = communicateWithServer();
            runOnUiThread(() -> {
                AccountAdapter adapter = new AccountAdapter(comptes);
                recyclerView.setAdapter(adapter);
            });
        }).start();
    }

    private List<Compte> communicateWithServer() {
        List<Compte> comptesList = new ArrayList<>();
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("10.0.2.2", 9090) // Adresse du serveur
                .usePlaintext()
                .build();

        try {
            CompteServiceGrpc.CompteServiceBlockingStub stub = CompteServiceGrpc.newBlockingStub(channel);

            // Récupérer les comptes
            GetAllComptesRequest request = GetAllComptesRequest.newBuilder().build();
            GetAllComptesResponse response = stub.allComptes(request);
            comptesList = response.getComptesList();

            // Récupérer les statistiques du solde
            GetTotalSoldeRequest statsRequest = GetTotalSoldeRequest.newBuilder().build();
            GetTotalSoldeResponse statsResponse = stub.totalSolde(statsRequest);

            SoldeStats stats = statsResponse.getStats();
            int count = stats.getCount();
            float sum = stats.getSum();
            float average = stats.getAverage();

            // Log pour déboguer les stats
            Log.d(TAG, "Total comptes: " + count + ", Total solde: " + sum + ", Moyenne: " + average);

            // Afficher les statistiques dans l'UI
            runOnUiThread(() -> {
                updateStats(count, sum, average);
            });

            // Retourner la liste de comptes
            return comptesList;

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la communication avec le serveur gRPC", e);
            return comptesList; // Retourner une liste vide en cas d'erreur
        } finally {
            channel.shutdown();
        }
    }

    private void updateStats(int count, float sum, float average) {
        TextView tvStats = findViewById(R.id.tvStats);
        String statsText = "Nombre de comptes: " + count + "\n" +
                "Solde total: " + sum + "€\n" +
                "Moyenne des soldes: " + average + "€";
        tvStats.setText(statsText);
    }
    private void showAddAccountDialog() {
        // Créez une instance de dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_account, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Récupérer les champs
        EditText etSolde = dialogView.findViewById(R.id.etSolde);
        Spinner spTypeCompte = dialogView.findViewById(R.id.spTypeCompte);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Initialiser le Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Courant", "Épargne"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTypeCompte.setAdapter(adapter);

        // Gérer le clic sur le bouton "Ajouter"
        btnSave.setOnClickListener(v -> {
            String soldeStr = etSolde.getText().toString().trim();
            String typeStr = spTypeCompte.getSelectedItem().toString();

            if (soldeStr.isEmpty() || typeStr.isEmpty()) {
                // Afficher une erreur si un champ est vide
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            } else {
                // Convertir les données et appeler l'API
                float solde = Float.parseFloat(soldeStr);
                TypeCompte typeCompte = typeStr.equals("Courant") ? TypeCompte.COURANT : TypeCompte.EPARGNE;
                addAccount(solde,getCurrentDate(), typeCompte);

                dialog.dismiss(); // Fermer le dialog
            }
        });
    }
    private String getCurrentDate() {
        // Utiliser Calendar pour obtenir la date actuelle
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); // Format ISO 8601
        return dateFormat.format(calendar.getTime());
    }
    private void addAccount(float solde, String dateCreation, TypeCompte type) {
        new Thread(() -> {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress("10.0.2.2", 9090) // Adresse du serveur
                    .usePlaintext()
                    .build();

            try {
                CompteServiceGrpc.CompteServiceBlockingStub stub = CompteServiceGrpc.newBlockingStub(channel);

                // Créer une requête pour ajouter un compte
                SaveCompteRequest request = SaveCompteRequest.newBuilder()
                        .setCompte(CompteRequest.newBuilder()
                                .setSolde(solde)
                                .setDateCreation(dateCreation)
                                .setType(type)
                                .build())
                        .build();

                // Envoyer la requête et récupérer la réponse
                SaveCompteResponse response = stub.saveCompte(request);
                Compte addedCompte = response.getCompte();

                runOnUiThread(() -> {
                    // Afficher les informations du compte ajouté dans un TextView ou RecyclerView
                    String message = "Compte ajouté avec succès:\n" +
                            "ID: " + addedCompte.getId() + "\n" +
                            "Solde: " + addedCompte.getSolde() + "\n" +
                            "Date: " + addedCompte.getDateCreation() + "\n" +
                            "Type: " + addedCompte.getType().name();
                    Log.d("AddAccount", message);

                    // Rafraîchir la liste des comptes
                    RecyclerView recyclerView = findViewById(R.id.recyclerViewAccounts);
                    List<Compte> comptes = communicateWithServer();
                    AccountAdapter adapter = new AccountAdapter(comptes);
                    recyclerView.setAdapter(adapter);
                });

            } catch (Exception e) {
                Log.e("AddAccount", "Erreur lors de l'ajout du compte", e);
            } finally {
                channel.shutdown();
            }
        }).start();
    }


}