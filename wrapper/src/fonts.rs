use crate::app::AppMessage;
use iced::font::{Family, Weight};
use iced::{font, Command, Font};

const FFFLAUTA_100_BYTES: &[u8] = include_bytes!("../resources/fonts/FFFlauta-100.ttf");
pub const FFFLAUTA_100: Font = Font::with_name("FFFlauta");

const GEIST_REGULAR_BYTES: &[u8] = include_bytes!("../resources/fonts/Geist-Regular.otf");
pub const GEIST_REGULAR: Font = Font {
    family: Family::Name("Geist"),
    weight: Weight::Normal,
    ..Font::DEFAULT
};

const GEIST_SEMIBOLD_BYTES: &[u8] = include_bytes!("../resources/fonts/Geist-SemiBold.otf");
pub const GEIST_SEMIBOLD: Font = Font {
    family: Family::Name("Geist"),
    weight: Weight::Semibold,
    ..Font::DEFAULT
};

pub fn load_fonts() -> Command<AppMessage> {
    Command::batch(vec![
        font::load(FFFLAUTA_100_BYTES),
        font::load(GEIST_REGULAR_BYTES),
        font::load(GEIST_SEMIBOLD_BYTES),
    ])
    .map(|_| AppMessage::Nothing)
}
