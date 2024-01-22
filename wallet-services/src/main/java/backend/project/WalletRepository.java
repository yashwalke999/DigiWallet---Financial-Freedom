package backend.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    Wallet findByUserName(String userName);

//    @Modifying
//    @Query(value = "UPDATE wallets SET balance = balance + :amount WHERE userName = :userName")
//    void updateWallet(String userName, int amount);
}
