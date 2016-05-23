package jdd.so.bot.actions.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberFilter {

	private Integer number;
	private Arithmetics arithmetics;

	public NumberFilter(String filter) {
		// 1. Try to find the arithmetics
		String regexFindAritmetics = "(?=(-(?<=\\D)))|(?=((?<=[^-])\\d))";
		String[] aa = filter.trim().split(regexFindAritmetics);
		if (aa.length > 1) {
			arithmetics = getArithmetics(aa[0].trim());
		}
		number = getNumber(filter);

	}

	public boolean isFilterActive() {
		return number != null;
	}

	private Arithmetics getArithmetics(String compare) {
		switch (compare) {
		case "=":
			return Arithmetics.EQUALS;
		case ">":
			return Arithmetics.MORE;
		case "<":
			return Arithmetics.LESS;
		case ">=":
			return Arithmetics.MORE_EQUALS;
		case "<=":
			return Arithmetics.LESS_EQUALS;
		default:
			return Arithmetics.NONE;
		}
	}

	private String getSign(Arithmetics arithmetics) {
		if (arithmetics == null) {
			return null;
		}
		switch (arithmetics) {
		case EQUALS:
			return "=";
		case MORE:
			return ">";
		case LESS:
			return "<";
		case MORE_EQUALS:
			return ">=";
		case LESS_EQUALS:
			return "<=";
		default:
			return null;
		}
	}

	public Integer getNumber(String filter) {
		Pattern regex = Pattern.compile("-?[0-9]+(?:,[0-9]+)?");
		Matcher m = regex.matcher(filter);
		if (m.find()) {
			try {
				return Integer.parseInt(m.group());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	public void validateCloseFilter() {

		if (getNumber() != null) {
			if (getNumber() <= 0) {
				setNumber(null);
			}
			if (getNumber() >= 4) {
				if (getArithmetics() == Arithmetics.MORE) {
					setNumber(3);
				} else {
					setNumber(4);
				}
			}
		}
	}

	public void validateDayFilter() {
		if (getNumber() != null) {
			setNumber(Math.abs(getNumber()));
		}

		if (getNumber() != null && getNumber() == 0) {
			setNumber(1);
		}
	}

	@Override
	public String toString() {
		if (!isFilterActive()) {
			return "Not valid";
		}
		String retVal = "";
		String sign = getSign(arithmetics);
		if (sign != null) {
			retVal += sign;
		}
		return retVal += String.valueOf(number);

	}

	public Integer getNumber() {
		return number;
	}

	public Arithmetics getArithmetics() {
		return arithmetics;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public void setArithmetics(Arithmetics arithmetics) {
		this.arithmetics = arithmetics;
	}

	public boolean inRange(int value) {
		if (number == null) {
			return true;
		}
		if (arithmetics == null) {
			arithmetics = Arithmetics.NONE;
		}
		switch (arithmetics) {
		case NONE:
		case EQUALS:
			return value == number.intValue();
		case LESS_EQUALS:
			return value <= number.intValue();
		case MORE_EQUALS:
			return value >= number.intValue();
		case LESS:
			return value < number.intValue();
		case MORE:
			return value > number.intValue();
		}
		return true;
	}

}
