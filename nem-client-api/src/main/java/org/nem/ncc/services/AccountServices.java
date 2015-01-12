package org.nem.ncc.services;

import org.nem.core.connect.HttpJsonPostRequest;
import org.nem.core.connect.client.NisApiId;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.*;
import org.nem.ncc.connector.PrimaryNisConnector;
import org.nem.ncc.controller.requests.AccountIdRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class provides higher-level functions around accessing accounts.
 */
public class AccountServices {
	private final PrimaryNisConnector nisConnector;

	/**
	 * Creates new account services.
	 *
	 * @param nisConnector The NIS connector.
	 */
	public AccountServices(final PrimaryNisConnector nisConnector) {
		this.nisConnector = nisConnector;
	}

	/**
	 * Gets account information for the specified account.
	 *
	 * @param address The account address.
	 * @return The account information.
	 */
	public AccountMetaDataPair getAccountMetaDataPair(final Address address) {
		final Deserializer deserializer = this.nisConnector.get(NisApiId.NIS_REST_ACCOUNT_LOOK_UP, formatIdQueryString(address, null));
		return new AccountMetaDataPair(deserializer);
	}

	/**
	 * Gets account information for the specified collections of accounts.
	 *
	 * @param requests The collection of account requests.
	 * @return The collection of account information.
	 */
	public Collection<AccountMetaDataPair> getAccountMetaDataPairs(final Collection<AccountIdRequest> requests) {
		final Deserializer deserializer = this.nisConnector.post(
				NisApiId.NIS_REST_ACCOUNT_BATCH_LOOK_UP,
				new HttpJsonPostRequest(new SerializableList<>(requests)));
		return new SerializableList<>(deserializer, AccountMetaDataPair::new).asCollection();
	}

	private NisApiId typeOfTransactionToQueryId(final TransactionDirection direction) {
		switch (direction) {
			case INCOMING:
				return NisApiId.NIS_REST_ACCOUNT_TRANSFERS_INCOMING;
			case OUTGOING:
				return NisApiId.NIS_REST_ACCOUNT_TRANSFERS_OUTGOING;
			default:
				return NisApiId.NIS_REST_ACCOUNT_TRANSFERS_ALL;
		}
	}

	/**
	 * Gets confirmed transactions for the specified account.
	 *
	 * @param direction Type of transactions.
	 * @param address The account address.
	 * @param transactionId The id of top-most transaction.
	 * @return The account information.
	 */
	public List<TransactionMetaDataPair> getTransactions(final TransactionDirection direction, final Address address, final Long transactionId) {
		final String queryString = formatIdQueryString(address, transactionId);
		final Deserializer deserializer = this.nisConnector.get(this.typeOfTransactionToQueryId(direction), queryString);
		return deserializer.readObjectArray("data", TransactionMetaDataPair::new);
	}

	/**
	 * Gets unconfirmed transactions for the specified account.
	 *
	 * @param address The account address.
	 * @return The account information.
	 */
	public List<Transaction> getUnconfirmedTransactions(final Address address) {
		final String queryString = formatIdQueryString(address, null);
		final Deserializer deserializer = this.nisConnector.get(NisApiId.NIS_REST_ACCOUNT_UNCONFIRMED, queryString);
		return deserializer.readObjectArray("data", TransactionFactory.VERIFIABLE).stream()
				.sorted((lhs, rhs) -> -1 * lhs.getTimeStamp().compareTo(rhs.getTimeStamp()))
				.collect(Collectors.toList());
	}

	/**
	 * Gets account harvests for the specified account.
	 *
	 * @param address The account address.
	 * @param endHash The hash of top-most harvest.
	 * @return The account information.
	 */
	public List<HarvestInfo> getAccountHarvests(final Address address, final Hash endHash) {
		final String queryString = formatHashQueryString(address, endHash);
		final Deserializer deserializer = this.nisConnector.get(NisApiId.NIS_REST_ACCOUNT_HARVESTS, queryString);
		return deserializer.readObjectArray("data", HarvestInfo::new);
	}

	/**
	 * Formats a string containing address and hash information.
	 *
	 * @param address The address.
	 * @param hash The hash.
	 * @return The formatted string.
	 */
	private static String formatHashQueryString(final Address address, final Hash hash) {
		final StringBuilder builder = new StringBuilder();
		builder.append("address=");
		builder.append(address.getEncoded());

		if (null != hash) {
			builder.append("&hash=");
			builder.append(hash);
		}

		return builder.toString();
	}

	/**
	 * Formats a string containing address and id information.
	 *
	 * @param address The address.
	 * @param id The id.
	 * @return The formatted string.
	 */
	private static String formatIdQueryString(final Address address, final Long id) {
		final StringBuilder builder = new StringBuilder();
		builder.append("address=");
		builder.append(address.getEncoded());

		if (null != id) {
			builder.append("&id=");
			builder.append(id);
		}

		return builder.toString();
	}
}
