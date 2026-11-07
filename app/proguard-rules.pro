# Remove os Log.d/v/i do release. A sincronização DJEN registra OAB e nome do
# advogado, e os adapters de rede despejam corpo de erro HTTP — nada disso deve
# chegar ao logcat de um aparelho em produção.
# Só tem efeito com o proguard-android-optimize.txt, que é o arquivo usado aqui.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# O Firestore desserializa por reflexão (DocumentSnapshot.toObject), então os
# DTOs de sincronização precisam manter nomes de campo e acessores.
-keep class com.obiterjus.data.sincronizacao.*FirestoreDto { *; }

# Stacktrace legível nos relatórios de erro sem expor o nome do arquivo original.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
