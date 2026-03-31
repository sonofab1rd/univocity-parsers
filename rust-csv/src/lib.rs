use csv::ReaderBuilder;
use jni::objects::{JByteArray, JClass, JObjectArray, JString};
use jni::sys::{jint, jlong, jobjectArray};
use jni::JNIEnv;

/// Parse CSV bytes and return the total number of records (rows).
///
/// This is used by the benchmark to measure raw parsing throughput without
/// the overhead of converting every field into a Java String.
///
/// JNI signature:
///   com.univocity.parsers.csv.RustCsvParser#countRowsNative(byte[]) -> long
#[no_mangle]
pub extern "system" fn Java_com_univocity_parsers_csv_RustCsvParser_countRowsNative<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: JByteArray<'local>,
) -> jlong {
    let bytes = match env.convert_byte_array(&data) {
        Ok(b) => b,
        Err(_) => return -1,
    };

    let mut reader = ReaderBuilder::new()
        .has_headers(false)
        .from_reader(bytes.as_slice());

    let mut count: i64 = 0;
    for result in reader.records() {
        if result.is_ok() {
            count += 1;
        }
    }
    count
}

/// Parse CSV bytes and return a Java 2-D String array: String[][].
///
/// Each outer element is a row; each inner element is a field value.
/// Returns null on error.
///
/// JNI signature:
///   com.univocity.parsers.csv.RustCsvParser#parseAllNative(byte[], int) -> String[][]
#[no_mangle]
pub extern "system" fn Java_com_univocity_parsers_csv_RustCsvParser_parseAllNative<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    data: JByteArray<'local>,
    expected_rows: jint,
) -> jobjectArray {
    let bytes = match env.convert_byte_array(&data) {
        Ok(b) => b,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut reader = ReaderBuilder::new()
        .has_headers(false)
        .from_reader(bytes.as_slice());

    // Collect all records first so we know the row count.
    let capacity = if expected_rows > 0 {
        expected_rows as usize
    } else {
        1024
    };
    let mut records: Vec<csv::StringRecord> = Vec::with_capacity(capacity);
    for result in reader.records() {
        match result {
            Ok(record) => records.push(record),
            Err(_) => return std::ptr::null_mut(),
        }
    }

    // Build the outer String[][] array.
    let string_array_class = match env.find_class("[Ljava/lang/String;") {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };
    let outer: JObjectArray = match env.new_object_array(
        records.len() as i32,
        &string_array_class,
        JObject::null(),
    ) {
        Ok(a) => a,
        Err(_) => return std::ptr::null_mut(),
    };

    for (row_idx, record) in records.iter().enumerate() {
        let inner: JObjectArray = match env.new_object_array(
            record.len() as i32,
            "java/lang/String",
            JObject::null(),
        ) {
            Ok(a) => a,
            Err(_) => return std::ptr::null_mut(),
        };

        for (col_idx, field) in record.iter().enumerate() {
            let jstr: JString = match env.new_string(field) {
                Ok(s) => s,
                Err(_) => return std::ptr::null_mut(),
            };
            if env
                .set_object_array_element(&inner, col_idx as i32, &jstr)
                .is_err()
            {
                return std::ptr::null_mut();
            }
        }

        if env
            .set_object_array_element(&outer, row_idx as i32, &inner)
            .is_err()
        {
            return std::ptr::null_mut();
        }
    }

    outer.into_raw()
}

// Bring JObject into scope for the null() calls above.
use jni::objects::JObject;
