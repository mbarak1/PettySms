<?php

// Database configuration
$servername = "127.0.0.1";
$username = "u490247366_mbarak";
$password = "";
$database = "u490247366_trucks1";

// Create connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Check if the request method is POST
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Check if the operation parameter is provided
    if (isset($_POST['operation'])) {
        // Extract the operation parameter
        $operation = $_POST['operation'];

        // Perform different operations based on the value of the operation parameter
        switch ($operation) {
            case 'getalltrucks':
                // Call a function to retrieve all trucks from the database
                $trucks = getAllTrucks($conn);
                echo json_encode($trucks);
                break;
            case 'gettruckbyno':
                // Check if the truck_no parameter is provided
                if (isset($_POST['truck_no'])) {
                    $truck_no = $_POST['truck_no'];
                    // Call a function to retrieve a truck by its truck_no from the database
                    $truck = getTruckByNo($conn, $truck_no);
                    echo json_encode($truck);
                } else {
                    // Return an error message if the truck_no parameter is missing
                    echo json_encode(array('error' => 'truck_no parameter is missing'));
                }
                break;

            case 'getallowners':
                $owners = getAllOwners($conn);
                echo json_encode($owners);
                break;

            case 'getallaccounts':
                $accounts = getAllAccounts($conn);
                echo json_encode($accounts);
                break;

            case 'sendToEasyQuickImport':
                // Check if all required parameters are provided
                if (
                    isset($_POST['pettyCashNumber']) && isset($_POST['amount']) &&
                    isset($_POST['description']) && isset($_POST['date']) &&
                    isset($_POST['accountName']) && isset($_POST['ownerName'])
                ) {

                    $result = sendToEasyQuickImport(
                        $conn,
                        $_POST['pettyCashNumber'],
                        $_POST['amount'],
                        $_POST['description'],
                        $_POST['date'],
                        $_POST['accountName'],
                        $_POST['ownerName'],
                        $_POST['truckNumbers'] ?? null
                    );

                    echo json_encode($result);
                } else {
                    // Return an error message if any required parameter is missing
                    echo json_encode(array('error' => 'Required parameters are missing'));
                }
                break;

            // Add more cases for other operations here
            default:
                // Return an error message if the operation is not supported
                echo json_encode(array('error' => 'Unsupported operation'));
                break;
        }
    } else {
        // Return an error message if the operation parameter is missing
        echo json_encode(array('error' => 'Operation parameter is missing'));
    }
} else {
    // Return an error message if the request method is not POST
    echo json_encode(array('error' => 'Only POST requests are allowed'));
}

// Function to retrieve all trucks from the database
function getAllTrucks($conn)
{
    $trucks = array();

    // Perform SQL query to retrieve all trucks from the database
    $sql = "SELECT * FROM trucks";
    $result = $conn->query($sql);

    // Check if any rows were returned
    if ($result->num_rows > 0) {
        // Fetch rows and add them to the $trucks array
        while ($row = $result->fetch_assoc()) {
            $trucks[] = $row;
        }
    }

    return $trucks;
}

// Function to retrieve all companies from the database
function getAllOwners($conn)
{
    $owners = array();

    // Perform SQL query to retrieve all trucks from the database
    $sql = "SELECT * FROM owners";
    $result = $conn->query($sql);

    // Check if any rows were returned
    if ($result->num_rows > 0) {
        // Fetch rows and add them to the $trucks array
        while ($row = $result->fetch_assoc()) {
            $owners[] = $row;
        }
    }

    return $owners;
}

// Function to retrieve a truck by its truck_no from the database
function getTruckByNo($conn, $truck_no)
{
    $truck = array();

    // Perform SQL query to retrieve a truck by its truck_no from the database
    $sql = "SELECT * FROM trucks WHERE truck_no = '$truck_no'";
    $result = $conn->query($sql);

    // Check if a row was returned
    if ($result->num_rows > 0) {
        // Fetch the row
        $truck = $result->fetch_assoc();
    }

    return $truck;
}

function getAllAccounts($conn): array
{

    $accounts = array();

    // SQL query to join the two tables and select the desired columns
    $sql = "
        SELECT qa.id, qu.company_name, qa.user_id, qa.full_name, qa.currency, qa.account_type, qa.special_account_type, qa.account_number
        FROM quickbooks_account qa
        JOIN quickbooks_user qu ON qa.qb_username = qu.qb_username
    ";

    $result = $conn->query($sql);

    // Check if any rows were returned
    if ($result->num_rows > 0) {
        // Fetch rows and add them to the $accounts array
        while ($row = $result->fetch_assoc()) {
            $accounts[] = $row;
        }
    }

    return $accounts;
}

/**
 * Get the QuickBooks username from the quickbooks_user table based on the owner's company name
 * 
 * @param mysqli $conn Database connection
 * @param string $companyName The company name to look up
 * @return string|null The QuickBooks username or null if not found
 */
function getQuickBooksUsername($conn, $companyName)
{
    $sql = "SELECT qb_username FROM quickbooks_user WHERE company_name = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $companyName);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        return $row['qb_username'];
    }

    return null;
}

/**
 * Check if a petty cash entry already exists in the quickbooks_queue table
 * 
 * @param mysqli $conn Database connection
 * @param string $pettyCashNumber The petty cash number to check
 * @param string $ownerName The owner name
 * @return bool True if entry exists, false otherwise
 */
function checkPettyCashExists($conn, $pettyCashNumber, $ownerName)
{
    try {
        // Check in quickbooks_queue table for existing entry with same petty cash number and owner
        $sql = "SELECT COUNT(*) as count FROM quickbooks_queue WHERE ident = ? AND qb_username = (SELECT qb_username FROM quickbooks_user WHERE company_name = ?) AND qb_status IN ('q', 'i', 'c')";
        $stmt = $conn->prepare($sql);
        $ident = "Petty SMS " . $pettyCashNumber;
        $stmt->bind_param("ss", $ident, $ownerName);
        $stmt->execute();
        $result = $stmt->get_result();
        $row = $result->fetch_assoc();

        return $row['count'] > 0;
    } catch (Exception $e) {
        error_log("Error checking petty cash existence: " . $e->getMessage());
        return false;
    }
}

/**
 * Send petty cash data to EasyQuickImport by adding to the quickbooks_queue table
 */
function sendToEasyQuickImport($conn, $pettyCashNumber, $amount, $description, $date, $accountName, $ownerName, $truckNumbers = null)
{
    try {
        // First check if this petty cash entry already exists
        if (checkPettyCashExists($conn, $pettyCashNumber, $ownerName)) {
            return array(
                "success" => false,
                "error" => "This petty cash entry ($pettyCashNumber) has already been queued or processed for $ownerName"
            );
        }

        // Get account ID by name
        $accountId = getAccountIdByName($conn, $accountName);
        if (!$accountId) {
            return array("success" => false, "error" => "Account not found: $accountName");
        }

        // Get QuickBooks username based on owner name
        $qbUsername = getQuickBooksUsername($conn, $ownerName);
        if (!$qbUsername) {
            return array("success" => false, "error" => "QuickBooks user not found for owner: $ownerName");
        }

        // Format date for QuickBooks (YYYY-MM-DD)
        $formattedDate = date('Y-m-d', strtotime($date));

        // Extract the middle number from pettyCashNumber for RefNumber
        $refNumber = $pettyCashNumber;
        if (preg_match('/\/(\d+)\/\d{4}$/', $pettyCashNumber, $matches)) {
            $refNumber = $matches[1];
        }

        $formattedAmount = number_format((float) $amount, 2, '.', '');

        // Create a unique identifier for this transaction
        $ident = "Petty SMS " . $pettyCashNumber;

        // Create the QBXML for the journal entry
        $qbxml = '<?xml version="1.0" encoding="utf-8"?>
                    <?qbxml version="13.0"?>
                    <QBXML>
                    <QBXMLMsgsRq onError="continueOnError">
                    <JournalEntryAddRq>
                    <JournalEntryAdd>
                    <TxnDate>' . $formattedDate . '</TxnDate>
                    <RefNumber>' . $refNumber . '</RefNumber>
                    <CurrencyRef>
                    <FullName>Kenyan Shilling</FullName>
                    </CurrencyRef>
                    <ExchangeRate>1</ExchangeRate>
                    <JournalDebitLine>
                    <AccountRef>
                    <FullName>' . htmlspecialchars($accountName) . '</FullName>
                    </AccountRef>
                    <Amount>' . $formattedAmount . '</Amount>
                    <Memo>' . htmlspecialchars($description) . '</Memo>
                    </JournalDebitLine>
                    <JournalCreditLine>
                    <AccountRef>
                    <FullName>Petty Cash</FullName>
                    </AccountRef>
                    <Amount>' . $formattedAmount . '</Amount>
                    <Memo>' . htmlspecialchars($description) . '</Memo>
                    </JournalCreditLine>
                    </JournalEntryAdd>
                    </JournalEntryAddRq>
                    </QBXMLMsgsRq>
                    </QBXML>';

        // Keep extra as empty string as it works better with the system
        $extra = "";

        // Insert into quickbooks_queue table
        $sql = "INSERT INTO quickbooks_queue 
                (qb_username, qb_action, ident, extra, qbxml, priority, qb_status, enqueue_datetime) 
                VALUES (?, 'JournalEntryAdd', ?, ?, ?, 0, 'q', NOW())";

        $stmt = $conn->prepare($sql);
        $stmt->bind_param("ssss", $qbUsername, $ident, $extra, $qbxml);

        if ($stmt->execute()) {
            return array("success" => true, "message" => "Successfully queued journal entry for QuickBooks");
        } else {
            return array("success" => false, "error" => "Failed to queue journal entry: " . $stmt->error);
        }
    } catch (Exception $e) {
        return array("success" => false, "error" => "Exception: " . $e->getMessage());
    }
}

/**
 * Helper function to get account ID by name
 */
function getAccountIdByName($conn, $accountName)
{
    $sql = "SELECT id FROM quickbooks_account WHERE full_name = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $accountName);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        return $row['id'];
    }

    return null;
}

// Close database connection
$conn->close();

