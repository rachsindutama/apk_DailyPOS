path = "app/src/main/java/com/example/ui/screens/StockMovementScreen.kt"
with open(path, "r") as f:
    content = f.read()

old_block = """                                 Button(
                                     onClick = {
                                         val prod = selectedProduct
                                         val qtyNum = qtyInput.toIntOrNull() ?: 0
                                         if (prod == null) {
                                             Toast.makeText(context, "Silakan pilih produk terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                             return@Button
                                         }
                                         if (qtyNum <= 0) {
                                             Toast.makeText(context, "Jumlah QTY harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                                             return@Button
                                         }"""

new_block = """                                 Button(
                                     onClick = {
                                         val prod = selectedProduct
                                         val qtyNum = qtyInput.toIntOrNull() ?: 0
                                         if (prod == null) {
                                             Toast.makeText(context, "Silakan pilih produk terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                             return@Button
                                         }
                                         if (!isStockIn) {
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

if old_block in content:
    content = content.replace(old_block, new_block, 1)
    with open(path, "w") as f:
        f.write(content)
    print("SUCCESS")
else:
    print("OLD BLOCK NOT FOUND")
