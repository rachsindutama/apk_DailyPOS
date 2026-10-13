path = "app/src/main/java/com/example/ui/screens/StockMovementScreen.kt"
with open(path, "r") as f:
    text = f.read()

# Let's find the exact pattern around prod and qtyNum
idx = text.find("val qtyNum = qtyInput.toIntOrNull() ?: 0")
if idx != -1:
    print("Found qtyNum at", idx)
    # let's inspect surrounding 200 chars
    print(text[idx:idx+250])
    
    target_snippet = """                                         if (qtyNum <= 0) {
                                             Toast.makeText(context, "Jumlah QTY harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                                             return@Button
                                         }"""
    
    replacement_snippet = """                                         if (!isStockIn) {
                                             if (prod.stock <= 0) {
                                                 Toast.makeText(context, f"Stok '{prod.name}' sudah habis (0)! Tidak dapat memproses stok keluar / NSB.", Toast.LENGTH_SHORT).show()
                                                 return@Button
                                             }
                                             if (qtyNum > prod.stock) {
                                                 Toast.makeText(context, f"Jumlah QTY ({qtyNum}) melebihi stok yang tersedia ({prod.stock} {prod.unit})!", Toast.LENGTH_SHORT).show()
                                                 return@Button
                                             }
                                         }
                                         if (qtyNum <= 0) {
                                             Toast.makeText(context, "Jumlah QTY harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                                             return@Button
                                         }"""
    
    if target_snippet in text:
        text = text.replace(target_snippet, replacement_snippet, 1)
        with open(path, "w") as f:
            f.write(text)
        print("REPLACED SUCCESS")
    else:
        print("TARGET SNIPPET NOT FOUND")
else:
    print("QTYNUM NOT FOUND")
