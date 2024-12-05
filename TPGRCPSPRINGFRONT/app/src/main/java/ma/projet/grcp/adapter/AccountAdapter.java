package ma.projet.grcp.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import ma.projet.grpc.stubs.Compte;
import ma.projet.grcp.R;


public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private List<Compte> accountList;

    public AccountAdapter(List<Compte> accountList) {
        this.accountList = accountList;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bank, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Compte account = accountList.get(position);

        // Formatter le numéro de compte
        holder.tvAccountId.setText("N°: " + account.getId());

        // Mettre en majuscules le type de compte
        holder.tvAccountType.setText(account.getType().toString().toUpperCase());

        // Formatter le solde avec le symbole de devise
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        holder.tvAccountBalance.setText(formatter.format(account.getSolde()));

        // Formatter la date
        holder.tvAccountDate.setText("Créé le " + account.getDateCreation());

        // Définir la couleur de la bande latérale en fonction du type de compte
        View colorStrip = holder.itemView.findViewById(R.id.colorStrip);
        if (account.getType().toString().equalsIgnoreCase("EPARGNE")) {
            colorStrip.setBackgroundColor(Color.parseColor("#4CAF50")); // Vert pour épargne
        } else if (account.getType().toString().equalsIgnoreCase("COURANT")) {
            colorStrip.setBackgroundColor(Color.parseColor("#2196F3")); // Bleu pour courant
        } else {
            colorStrip.setBackgroundColor(Color.parseColor("#FF9800")); // Orange pour autres
        }
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvAccountId, tvAccountType, tvAccountBalance, tvAccountDate;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAccountId = itemView.findViewById(R.id.tvAccountId);
            tvAccountType = itemView.findViewById(R.id.tvAccountType);
            tvAccountBalance = itemView.findViewById(R.id.tvAccountBalance);
            tvAccountDate = itemView.findViewById(R.id.tvAccountDate);
        }
    }
}
